import { useState, useEffect, FormEvent, useRef, useMemo } from 'react';
import { MessageSquare, X, Send, Loader2 } from 'lucide-react';
import { collection, query, orderBy, onSnapshot, Timestamp, setDoc, doc, addDoc, serverTimestamp } from 'firebase/firestore';
import axiosClient from '../../api/axiosClient';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { db } from '../../lib/firebase';
import { useReadReceipts } from '../../hooks/useReadReceipts';

interface OnlineAgent { uid: string; firstName: string; }
interface LiveChatMessage {
    id: string;
    content: string;
    senderId: string;
    timestamp: Timestamp | null;
    isRead: boolean;
}

const ChatBubble = () => {
    const { currentUser, appUser, loading: authLoading, stompClient } = useAuth();
    const navigate = useNavigate();
    const [isOpen, setIsOpen] = useState(false);
    const [onlineAgents, setOnlineAgents] = useState<OnlineAgent[]>([]);
    const [view, setView] = useState<'loading' | 'bot' | 'live_chat' | 'auth_prompt' | 'offline_form'>('loading');
    const [liveMessages, setLiveMessages] = useState<LiveChatMessage[]>([]);
    const [activeChatId, setActiveChatId] = useState<string | null>(null);
    const [inputMessage, setInputMessage] = useState('');
    const [isSending, setIsSending] = useState(false);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useReadReceipts(liveMessages, activeChatId, currentUser?.uid);

    const botMessages = useMemo(() => ({
        start: { text: "Hi there! 👋 How can I help you?", replies: [{ text: "Products", action: 'navigate_products' }, { text: "Services", action: 'navigate_services'}, { text: "I have a question", action: 'ask_question' }] },
        auth_prompt: { text: "To ask a question and keep track of your conversation, please sign in or create an account." },
        offline_prompt_user: { text: "All our agents are currently offline. Leave a message here and we'll get back to you as soon as possible." }
    }), []);

    const activeChatAgent = useMemo(() => onlineAgents[0] || { uid: 'default_support_uid', firstName: 'Support' }, [onlineAgents]);

    useEffect(() => {
        const fetchAgents = async () => {
            if (authLoading || !currentUser) return;
            try {
                const res = await axiosClient.get<OnlineAgent[]>('/stats/support/online-agents');
                setOnlineAgents(res.data);
            } catch { setOnlineAgents([]); }
        };
        void fetchAgents();
        const interval = setInterval(fetchAgents, 30000);
        return () => clearInterval(interval);
    }, [authLoading, currentUser]);

    useEffect(() => {
        if (!isOpen || authLoading) { setView('loading'); return; }
        const startOrResumeChat = async () => {
            if (currentUser) {
                try {
                    const res = await axiosClient.get<{ chatId: string }>('/chats/my-active-conversation');
                    if (res.status === 200 && res.data.chatId) {
                        setActiveChatId(res.data.chatId);
                        setView('live_chat');
                    } else { setView('bot'); }
                } catch { setView('bot'); }
            } else { setView('bot'); }
        };
        void startOrResumeChat();
    }, [isOpen, currentUser, authLoading]);

    useEffect(() => {
        if (!activeChatId) { setLiveMessages([]); return; }
        const messagesQuery = query(collection(db, 'chats', activeChatId, 'messages'), orderBy('timestamp'));
        const unsubscribe = onSnapshot(messagesQuery, (snapshot) => {
            setLiveMessages(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as LiveChatMessage)));
        });
        return () => unsubscribe();
    }, [activeChatId]);

    useEffect(() => {
        if (isOpen && currentUser && stompClient?.connected) {
            console.log('Chat ouvert, vérification du statut de présence...');

            const presenceSub = stompClient.subscribe('/topic/presence', (message) => {
                console.log('Mise à jour de présence reçue dans ChatBubble:', message.body);
            });

            return () => {
                if (presenceSub) presenceSub.unsubscribe();
            };
        }
    }, [isOpen, currentUser, stompClient?.connected]);

    useEffect(() => {
        if (isOpen && currentUser) {
            axiosClient.post('/presence/heartbeat').catch(console.error);
        }
    }, [isOpen, currentUser]);

    useEffect(() => { messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [liveMessages]);

    const handleQuickReplyClick = (reply: { text: string; action: string; }) => {
        if (reply.action.startsWith('navigate')) {
            navigate(reply.action === 'navigate_products' ? '/products' : '/services');
            setIsOpen(false);
        } else if (reply.action === 'ask_question') {
            setView(currentUser ? 'live_chat' : 'auth_prompt');
        }
    };

    const handleSendMessage = async (e: FormEvent) => {
        e.preventDefault();
        if (!inputMessage.trim() || !currentUser) return;
        const currentMessage = inputMessage;
        setInputMessage('');
        setIsSending(true);

        const targetAgent = activeChatAgent || { uid: 'default_support_uid', firstName: 'Support' };
        const chatId = activeChatId || [currentUser.uid, targetAgent.uid].sort().join('_');
        if (!activeChatId) setActiveChatId(chatId);

        try {
            await addDoc(collection(db, 'chats', chatId, 'messages'), {
                content: currentMessage, senderId: currentUser.uid, receiverId: targetAgent.uid, timestamp: serverTimestamp(), isRead: false
            });

            const senderName = (`${appUser?.firstName || ''} ${appUser?.lastName || ''}`).trim() || currentUser.displayName || 'Client';

            await setDoc(doc(db, 'chats', chatId), {
                participant_uids: [currentUser.uid, targetAgent.uid],
                participant_names: {
                    [currentUser.uid]: senderName,
                    [targetAgent.uid]: targetAgent.firstName
                },
                status: 'OPEN',
                lastActivity: serverTimestamp()
            }, { merge: true });

        } catch (error) {
            console.error("Failed to send message:", error);
            setInputMessage(currentMessage);
        } finally {
            setIsSending(false);
        }
    };

    const handleOfflineSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const formData = new FormData(e.currentTarget);
        try {
            await axiosClient.post('/chats/offline-message', {
                name: formData.get('name') as string,
                email: formData.get('email') as string,
                message: formData.get('message') as string
            });
            alert("Thank you! Your message has been sent.");
            setIsOpen(false);
        } catch (error) {
            console.error("Error sending offline message:", error);
            alert("Sorry, an error occurred.");
        }
    };

    return (
        <>
            {isOpen && (
                <div className="fixed bottom-20 right-5 z-50 w-[350px] h-[500px] bg-white rounded-lg shadow-2xl flex flex-col">
                    <header className="bg-blue-600 p-3 flex items-center text-white rounded-t-lg">
                        <div className="mr-3 h-10 w-10 p-1 bg-white/20 rounded-full flex items-center justify-center font-bold">S</div>
                        <div>
                            <h3 className="font-bold">Support</h3>
                            <p className="text-xs">{onlineAgents.length > 0 ? "We're online" : "We're currently offline"}</p>
                        </div>
                    </header>

                    <main className="flex-grow p-3 overflow-y-auto space-y-3">
                        {(view === 'loading' || authLoading) && <Loader2 className="animate-spin m-auto" />}

                        {!authLoading && (view === 'bot' || view === 'auth_prompt') && (
                            <div className="flex items-end gap-2">
                                <div className="w-7 h-7 rounded-full bg-gray-200 flex items-center justify-center text-lg self-end shrink-0">🤖</div>
                                <div className="py-2 px-3 rounded-xl max-w-[85%] text-sm bg-gray-200 text-black">
                                    {view === 'bot' ? botMessages.start.text : botMessages.auth_prompt.text}
                                </div>
                            </div>
                        )}

                        {!authLoading && view === 'live_chat' && (
                            <>
                                {onlineAgents.length === 0 && <div className="text-xs text-center text-gray-500 p-2 bg-gray-100 rounded-md">Our agents are offline, but we'll reply as soon as possible.</div>}
                                {liveMessages.map(msg => (
                                    <div key={msg.id} id={`message-${msg.id}`} className={`flex items-end gap-2 text-sm ${msg.senderId === currentUser?.uid ? 'justify-end' : 'justify-start'}`}>
                                        {msg.senderId !== currentUser?.uid && <div className="h-6 w-6 p-1 bg-gray-200 rounded-full self-end shrink-0">{activeChatAgent?.firstName?.charAt(0) || 'A'}</div>}
                                        <div className={`py-2 px-3 rounded-2xl max-w-[75%] ${msg.senderId === currentUser?.uid ? 'bg-blue-600 text-white rounded-br-none' : 'bg-gray-200 text-black rounded-bl-none'}`}>{msg.content}</div>
                                    </div>
                                ))}
                            </>
                        )}
                        <div ref={messagesEndRef} />
                    </main>

                    <footer className="border-t">
                        {!authLoading && view === 'bot' && (
                            <div className="p-2 bg-gray-50 rounded-b-lg flex flex-wrap gap-2 justify-center">
                                {botMessages.start.replies?.map((reply, index) => (
                                    <button key={index} onClick={() => handleQuickReplyClick(reply)} className="bg-blue-100 text-blue-800 text-xs font-semibold px-3 py-1.5 rounded-full hover:bg-blue-200">{reply.text}</button>
                                ))}
                            </div>
                        )}

                        {!authLoading && view === 'auth_prompt' && (
                            <div className="p-4 bg-gray-50 rounded-b-lg space-y-2">
                                <button onClick={() => { navigate('/login'); setIsOpen(false); }} className="w-full bg-blue-600 text-white font-bold py-2 rounded-md">Se connecter</button>
                                <button onClick={() => { navigate('/register'); setIsOpen(false); }} className="w-full bg-gray-200 text-black font-bold py-2 rounded-md">S'inscrire</button>
                            </div>
                        )}

                        {!authLoading && view === 'live_chat' && (
                            <form onSubmit={handleSendMessage} className="p-2 flex items-center gap-2">
                                <input value={inputMessage} onChange={(e) => setInputMessage(e.target.value)} className="w-full bg-gray-100 p-2 rounded-lg text-sm" placeholder="Type a message..." disabled={isSending} />
                                <button type="submit" className="text-blue-600 p-2" disabled={isSending || !inputMessage.trim()}>
                                    {isSending ? <Loader2 size={20} className="animate-spin"/> : <Send size={20}/>}
                                </button>
                            </form>
                        )}

                        {view === 'offline_form' && (
                            <form onSubmit={handleOfflineSubmit} className="p-3 space-y-2 bg-gray-50 rounded-b-lg">
                                <input name="name" type="text" placeholder="Name" required className="w-full p-2 border rounded-md text-sm" />
                                <input name="email" type="email" placeholder="Email" required className="w-full p-2 border rounded-md text-sm" />
                                <textarea name="message" required placeholder="Your message..." className="w-full p-2 border rounded-md text-sm h-20"/>
                                <button type="submit" className="w-full bg-blue-600 text-white font-bold py-2 rounded-md">Send Message</button>
                            </form>
                        )}
                    </footer>
                </div>
            )}

            <button
                onClick={() => setIsOpen(p => !p)}
                className="bg-blue-600 text-white rounded-full p-4 shadow-lg fixed bottom-5 right-5 z-40 hover:scale-110 transition-transform"
            >
                {isOpen ? <X/> : <MessageSquare/>}
            </button>
        </>
    );
};

export default ChatBubble;