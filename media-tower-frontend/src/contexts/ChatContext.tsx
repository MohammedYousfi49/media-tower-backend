/*
/!* eslint-disable react-refresh/only-export-components *!/
import { createContext, useState, useEffect, useContext, ReactNode, useCallback } from 'react';
import { useAuth } from './AuthContext';
import { collection, query, where, onSnapshot, orderBy, addDoc, serverTimestamp, Timestamp } from 'firebase/firestore';
import { db } from '../lib/firebase';
import axiosClient from '../api/axiosClient';

// --- Types ---
export interface OnlineAgent { uid: string; firstName: string; lastName: string; }
export interface ChatMessage { id: string; content: string; senderId: string; timestamp: Timestamp; isRead: boolean; }
export interface Conversation { id: string; agent: OnlineAgent; messages: ChatMessage[]; }

interface ChatContextType {
    onlineAgents: OnlineAgent[];
    conversations: Conversation[];
    activeChatWindows: Conversation[];
    startOrOpenConversation: (agent: OnlineAgent) => void;
    openChatWindow: (conversation: Conversation) => void;
    closeChatWindow: (chatId: string) => void;
    sendMessage: (chatId: string, content: string) => Promise<void>;
}

const ChatContext = createContext<ChatContextType | undefined>(undefined);

export const useChat = () => {
    const context = useContext(ChatContext);
    if (!context) throw new Error("useChat must be used within a ChatProvider");
    return context;
};

export const ChatProvider = ({ children }: { children: ReactNode }) => {
    const { currentUser, appUser, stompClient } = useAuth();
    const [onlineAgents, setOnlineAgents] = useState<OnlineAgent[]>([]);
    const [conversations, setConversations] = useState<Conversation[]>([]);
    const [activeChatWindows, setActiveChatWindows] = useState<Conversation[]>([]);

    const cleanupListeners = useCallback((listeners: (() => void)[]) => {
        listeners.forEach(unsub => unsub());
    }, []);

    useEffect(() => {
        if (!stompClient?.connected) return;
        const sub = stompClient.subscribe('/topic/support/status', (message) => setOnlineAgents(JSON.parse(message.body)));
        if(stompClient.connected) {
            stompClient.send('/app/support/status');
        }
        return () => {
            if(sub) sub.unsubscribe();
        };
    }, [stompClient]);

    useEffect(() => {
        let currentListeners: (() => void)[] = [];
        if (!currentUser) {
            setConversations([]);
            return;
        }

        const chatsRef = collection(db, "chats");
        const q = query(chatsRef, where("participant_uids", "array-contains", currentUser.uid));

        const unsubConversations = onSnapshot(q, (snapshot) => {
            cleanupListeners(currentListeners);
            const newListeners: (() => void)[] = [];

            if(snapshot.empty) {
                setConversations([]);
                return;
            }

            const convPromises = snapshot.docs.map(docSnapshot => {
                const data = docSnapshot.data();
                const agentUid = data.participant_uids.find((uid: string) => uid !== currentUser.uid);
                if (!agentUid || agentUid === "default_support_uid") return null;

                const agentInfo = { uid: agentUid, firstName: data.participant_names?.[agentUid] || 'Support', lastName: 'Agent' };
                const messagesRef = collection(db, "chats", docSnapshot.id, "messages");
                const messagesQuery = query(messagesRef, orderBy("timestamp"));

                return new Promise<Conversation>((resolve) => {
                    const unsubMessages = onSnapshot(messagesQuery, (msgSnapshot) => {
                        const messages = msgSnapshot.docs.map(d => ({ id: d.id, ...d.data() } as ChatMessage));
                        resolve({ id: docSnapshot.id, agent: agentInfo, messages });
                    });
                    newListeners.push(unsubMessages);
                });
            });

            Promise.all(convPromises).then(resolvedConversations => {
                const filteredConvos = resolvedConversations.filter(Boolean) as Conversation[];
                setConversations(filteredConvos.sort((a,b) => (b.messages.slice(-1)[0]?.timestamp?.toMillis() || 0) - (a.messages.slice(-1)[0]?.timestamp?.toMillis() || 0)));
                currentListeners = newListeners;
            });
        });

        return () => {
            unsubConversations();
            cleanupListeners(currentListeners);
        };
    }, [currentUser, cleanupListeners]);

    const openChatWindow = (conversation: Conversation) => {
        setActiveChatWindows(prev => {
            if (prev.some(c => c.id === conversation.id)) return prev;
            return [...prev, conversation].slice(-3);
        });
    };

    const closeChatWindow = (chatId: string) => {
        setActiveChatWindows(prev => prev.filter(c => c.id !== chatId));
    };

    const sendMessage = async (chatId: string, content: string) => {
        const convo = conversations.find(c => c.id === chatId);
        if (!currentUser || !convo) return;
        const messagesRef = collection(db, "chats", chatId, "messages");
        await addDoc(messagesRef, { content, senderId: currentUser.uid, receiverId: convo.agent.uid, timestamp: serverTimestamp(), isRead: false });
    };

    const startOrOpenConversation = (agent: OnlineAgent) => {
        if (!currentUser || !appUser) return;
        const chatId = [currentUser.uid, agent.uid].sort().join('_');
        const existingConvo = conversations.find(c => c.id === chatId);

        if (existingConvo) {
            openChatWindow(existingConvo);
        } else {
            void axiosClient.post('/chats/start', { agentUid: agent.uid });
        }
    };

    const value = { onlineAgents, conversations, startOrOpenConversation, activeChatWindows, openChatWindow, closeChatWindow, sendMessage };

    return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
};*/
