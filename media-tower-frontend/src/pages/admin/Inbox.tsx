import { useEffect, useState, useMemo, FormEvent, useCallback, useRef, ChangeEvent } from 'react';
import axiosClient from '../../api/axiosClient';
import { useAuth } from '../../hooks/useAuth';
import { db } from '../../lib/firebase';
import { collection, query, orderBy, onSnapshot, addDoc, serverTimestamp, Timestamp, setDoc, doc } from 'firebase/firestore';
import { Send, Check, CheckCheck, Loader2 } from 'lucide-react';
import { Subscription } from 'stompjs';

interface FirestoreTimestamp {
    seconds: number;
    nanoseconds: number;
}

interface ClientInfo {
    id: string;
    name: string;
    isOnline: boolean;
    email: string;
}

interface ConversationAdmin {
    chatId: string;
    client: ClientInfo;
    lastMessage: string;
    lastMessageTimestamp: FirestoreTimestamp | string | null;
    unreadCount: number;
    status: 'OPEN' | 'CLOSED';
    assignedAdminName?: string;
}

interface ChatMessage {
    id: string;
    content: string;
    senderId: string;
    timestamp: Timestamp | null;
    status: 'sent' | 'delivered' | 'read';
    isRead: boolean;
}

const MessageStatus = ({ status }: { status: ChatMessage['status'] }) => {
    if (status === 'read') return <CheckCheck size={16} className="text-blue-400" />;
    if (status === 'delivered') return <CheckCheck size={16} className="text-gray-500" />;
    return <Check size={16} className="text-gray-500" />;
};

const formatTimestamp = (ts: FirestoreTimestamp | string | null): string => {
    if (!ts) return '';
    const date = new Date(typeof ts === 'string' ? ts : ts.seconds * 1000);
    return isNaN(date.getTime()) ? '' : date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

const Inbox = () => {
    const [allConversations, setAllConversations] = useState<ConversationAdmin[]>([]);
    const [isLoadingInitial, setIsLoadingInitial] = useState(true);
    const [activeFilter, setActiveFilter] = useState<'OPEN' | 'CLOSED'>('OPEN');
    const [activeConversation, setActiveConversation] = useState<ConversationAdmin | null>(null);
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [inputMessage, setInputMessage] = useState('');
    const { currentUser, stompClient } = useAuth();
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const loadConversations = async () => {
            try {
                const res = await axiosClient.get<ConversationAdmin[]>('/chats/admin/inbox');
                setAllConversations(res.data);
            } catch (error) {
                console.error('Error loading conversations:', error);
                setAllConversations([]);
            } finally {
                setIsLoadingInitial(false);
            }
        };

        loadConversations();
    }, []);

    useEffect(() => {
        if (!stompClient?.connected) return;

        const inboxSub = stompClient.subscribe('/topic/admin/inbox/update', (message) => {
            const updatedConvo = JSON.parse(message.body) as ConversationAdmin;
            setAllConversations(prev => {
                const convos = prev.filter(c => c.chatId !== updatedConvo.chatId);
                convos.unshift(updatedConvo);
                return convos.sort((a, b) => {
                    const aTime = new Date(a.lastMessageTimestamp as string).getTime();
                    const bTime = new Date(b.lastMessageTimestamp as string).getTime();
                    return bTime - aTime;
                });
            });
        });

        const presenceSub = stompClient.subscribe('/topic/presence', (message) => {
            const onlineStatusMap = JSON.parse(message.body);
            setAllConversations(prev =>
                prev.map(convo => ({
                    ...convo,
                    client: { ...convo.client, isOnline: onlineStatusMap[convo.client.id] || false }
                }))
            );
        });

        return () => {
            inboxSub.unsubscribe();
            presenceSub.unsubscribe();
        };
    }, [stompClient]);

    useEffect(() => {
        if (!activeConversation) return;

        let messagesUnsubscribe: (() => void) | null = null;
        let statusSub: Subscription | null = null;
        let readSub: Subscription | null = null;
        let messagesSub: Subscription | null = null;

        const chatId = activeConversation.chatId;

        const q = query(collection(db, 'chats', chatId, 'messages'), orderBy('timestamp'));
        messagesUnsubscribe = onSnapshot(q, snapshot => {
            setMessages(snapshot.docs.map(doc => {
                const data = doc.data();
                return { id: doc.id, ...data, status: data.isRead ? 'read' : 'sent' } as ChatMessage;
            }));
        });

        if (stompClient?.connected) {
            statusSub = stompClient.subscribe(`/topic/chat/${chatId}/status-update`, (message) => {
                const { messageId, status } = JSON.parse(message.body);
                setMessages(prev => prev.map(msg => msg.id === messageId && msg.status !== 'read' ? { ...msg, status } : msg));
            });

            readSub = stompClient.subscribe(`/topic/chat/${chatId}/read-receipt`, (message) => {
                const { readMessageIds } = JSON.parse(message.body);
                const readSet = new Set(readMessageIds);
                setMessages(prev => prev.map(msg => readSet.has(msg.id) ? { ...msg, status: 'read' } : msg));
            });

            // NEW: Subscribe to real-time messages for the current user
            messagesSub = stompClient.subscribe(`/user/${currentUser?.uid}/queue/messages`, (message) => {
                const newMessage = JSON.parse(message.body) as ChatMessage;
                // Only add if it's for the active conversation and not from the current user (already added via Firestore listener)
                if (newMessage.senderId !== currentUser?.uid && newMessage.senderId === activeConversation?.client.id) {
                    setMessages(prev => [...prev, newMessage]);
                }
            });
        }

        return () => {
            if (messagesUnsubscribe) messagesUnsubscribe();
            if (statusSub) statusSub.unsubscribe();
            if (readSub) readSub.unsubscribe();
            if (messagesSub) messagesSub.unsubscribe(); // Unsubscribe from messages topic on unmount/dependency change
        };
    }, [activeConversation, stompClient, currentUser?.uid]);

    const markConversationAsRead = useCallback((convo: ConversationAdmin | null) => {
        if (convo && convo.unreadCount > 0) {
            setAllConversations(prev => prev.map(c => c.chatId === convo.chatId ? { ...c, unreadCount: 0 } : c));
            axiosClient.post(`/chats/${convo.chatId}/mark-as-read`).catch(console.error);
        }
    }, []);

    const handleConversationSelect = (convo: ConversationAdmin) => {
        if (activeConversation?.chatId === convo.chatId) return;
        setActiveConversation(convo);
        markConversationAsRead(convo);
    };

    const handleSendMessage = async (e: FormEvent) => {
        e.preventDefault();
        if (!inputMessage.trim() || !currentUser || !activeConversation) return;
        try {
            await addDoc(collection(db, 'chats', activeConversation.chatId, 'messages'), {
                content: inputMessage,
                senderId: currentUser.uid,
                receiverId: activeConversation.client.id,
                timestamp: serverTimestamp(),
                isRead: false
            });
            await setDoc(doc(db, 'chats', activeConversation.chatId), {
                lastActivity: serverTimestamp()
            }, { merge: true });
            setInputMessage('');
        } catch (error) {
            console.error("Failed to send message:", error);
        }
    };

    const filteredConversations = useMemo(() => allConversations.filter(c => c.status === activeFilter), [allConversations, activeFilter]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    return (
        <div className="flex h-full bg-white dark:bg-card rounded-lg border dark:border-border shadow-lg">
            <div className="w-1/3 border-r dark:border-border flex flex-col">
                <div className="p-2 border-b">
                    <div className="flex bg-gray-100 p-1 rounded-md">
                        <button
                            onClick={() => setActiveFilter('OPEN')}
                            className={`w-1/2 p-1.5 text-sm font-bold ${activeFilter === 'OPEN' ? 'bg-blue-500 text-white' : 'text-gray-500'}`}
                        >
                            Active
                        </button>
                        <button
                            onClick={() => setActiveFilter('CLOSED')}
                            className={`w-1/2 p-1.5 text-sm font-bold ${activeFilter === 'CLOSED' ? 'bg-blue-500 text-white' : 'text-gray-500'}`}
                        >
                            History
                        </button>
                    </div>
                </div>
                <div className="overflow-y-auto">
                    {isLoadingInitial ? (
                        <div className="flex justify-center items-center h-full">
                            <Loader2 className="animate-spin text-gray-400" />
                        </div>
                    ) : (
                        filteredConversations.map(convo => (
                            <div
                                key={convo.chatId}
                                onClick={() => handleConversationSelect(convo)}
                                className={`flex items-start p-3 cursor-pointer border-l-4 ${
                                    activeConversation?.chatId === convo.chatId
                                        ? 'bg-blue-50/50 border-blue-500'
                                        : convo.unreadCount > 0
                                            ? 'border-blue-500'
                                            : 'border-transparent hover:bg-gray-50'
                                }`}
                            >
                                <div className="relative mr-3">
                                    <div className="w-10 h-10 flex items-center justify-center bg-pink-200 text-pink-600 rounded-full font-bold uppercase">
                                        {(convo.client?.name || '?').charAt(0)}
                                    </div>
                                    {convo.client?.isOnline && (
                                        <span className="absolute bottom-0 right-0 block h-3 w-3 rounded-full bg-green-500 ring-2 ring-white"></span>
                                    )}
                                </div>
                                <div className="flex-grow overflow-hidden">
                                    <div className="flex justify-between items-center">
                                        <p className="truncate text-sm font-semibold">
                                            {convo.client?.name || 'User'}
                                        </p>
                                        <p className="text-xs text-gray-400 ml-2">
                                            {formatTimestamp(convo.lastMessageTimestamp)}
                                        </p>
                                    </div>
                                    <div className="flex justify-between items-center">
                                        <p className={`text-sm truncate ${convo.unreadCount > 0 ? 'font-semibold' : 'text-gray-500'}`}>
                                            {convo.lastMessage}
                                        </p>
                                        {convo.unreadCount > 0 && (
                                            <span className="bg-blue-500 text-white text-xs font-bold rounded-full h-5 w-5 flex items-center justify-center ml-2">
                                                {convo.unreadCount}
                                            </span>
                                        )}
                                    </div>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>
            <div className="flex-1 flex flex-col">
                {activeConversation ? (
                    <>
                        <header className="p-4 border-b">
                            <div>
                                <h3 className="font-bold">
                                    {activeConversation.client.name} ({activeConversation.client.email})
                                </h3>
                                <p className={`text-xs flex items-center ${activeConversation.client.isOnline ? 'text-green-500' : 'text-gray-400'}`}>
                                    <span className={`h-2 w-2 rounded-full mr-1.5 ${activeConversation.client.isOnline ? 'bg-green-500' : 'bg-gray-400'}`}></span>
                                    {activeConversation.client.isOnline ? 'Online' : 'Offline'}
                                </p>
                            </div>
                        </header>
                        <div className="flex-grow p-4 overflow-y-auto space-y-2 bg-gray-100">
                            {messages.map(msg => (
                                <div
                                    key={msg.id}
                                    className={`flex flex-col ${msg.senderId === currentUser?.uid ? 'items-end' : 'items-start'}`}
                                >
                                    <div className={`py-2 px-3 rounded-2xl max-w-[70%] ${msg.senderId === currentUser?.uid ? 'bg-blue-500 text-white' : 'bg-white'}`}>
                                        {msg.content}
                                    </div>
                                    {msg.senderId === currentUser?.uid && (
                                        <div className="mt-1 px-1">
                                            <MessageStatus status={msg.status} />
                                        </div>
                                    )}
                                </div>
                            ))}
                            <div ref={messagesEndRef} />
                        </div>
                        <form onSubmit={handleSendMessage} className="p-3 border-t flex items-center gap-2">
                            <input
                                onFocus={() => markConversationAsRead(activeConversation)}
                                value={inputMessage}
                                onChange={(e: ChangeEvent<HTMLInputElement>) => setInputMessage(e.target.value)}
                                className="w-full bg-gray-100 p-3 rounded-lg outline-none"
                                placeholder="Write a message..."
                            />
                            <button
                                type="submit"
                                className="bg-blue-500 text-white p-3 rounded-full"
                                disabled={!inputMessage.trim()}
                            >
                                <Send size={20} />
                            </button>
                        </form>
                    </>
                ) : (
                    <div className="flex h-full items-center justify-center text-gray-500">
                        <p>Select a conversation</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default Inbox;