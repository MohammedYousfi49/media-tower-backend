/*
import { useState, useEffect, useRef, FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { db } from '../../lib/firebase';
import { collection, query, orderBy, onSnapshot, addDoc, serverTimestamp, doc, getDoc, Timestamp } from 'firebase/firestore';
import { Send, User, ArrowLeft } from 'lucide-react';

// --- Types ---
interface ChatMessage {
    id: string;
    content: string;
    senderId: string;
    timestamp: Timestamp;
}
interface AgentInfo {
    id: string;
    firstName: string;
}

const ChatPage = () => {
    const { chatId } = useParams<{ chatId: string }>();
    const { currentUser } = useAuth();
    const navigate = useNavigate();
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [agent, setAgent] = useState<AgentInfo | null>(null);
    const [inputMessage, setInputMessage] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!chatId) return;

        // Récupérer les informations de la conversation
        const chatRef = doc(db, 'chats', chatId);
        getDoc(chatRef).then(docSnap => {
            if (docSnap.exists() && currentUser) {
                const data = docSnap.data();
                const agentUid = data.participant_uids.find((uid: string) => uid !== currentUser.uid);
                if(agentUid) {
                    setAgent({ id: agentUid, firstName: data.participant_names[agentUid] || 'Support' });
                }
            }
            setIsLoading(false);
        });

        // Écouter les messages
        const messagesRef = collection(db, 'chats', chatId, 'messages');
        const q = query(messagesRef, orderBy('timestamp'));
        const unsubscribe = onSnapshot(q, (snapshot) => {
            setMessages(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as ChatMessage)));
        });

        return () => unsubscribe();
    }, [chatId, currentUser]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const handleSendMessage = async (e: FormEvent) => {
        e.preventDefault();
        if (!inputMessage.trim() || !chatId || !currentUser || !agent) return;

        const messagesRef = collection(db, 'chats', chatId, 'messages');
        await addDoc(messagesRef, {
            content: inputMessage,
            senderId: currentUser.uid,
            receiverId: agent.id,
            timestamp: serverTimestamp(),
            isRead: false
        });
        setInputMessage('');
    };

    if (isLoading) {
        return <div className="text-center p-8">Loading chat...</div>;
    }

    return (
        <div className="flex flex-col h-[calc(100vh-120px)] max-w-4xl mx-auto my-4 border rounded-lg bg-card shadow-lg">
            <div className="bg-primary p-4 flex items-center text-white rounded-t-lg">
                <button onClick={() => navigate(-1)} className="mr-4 p-2 hover:bg-white/20 rounded-full">
                    <ArrowLeft />
                </button>
                <User className="mr-3" />
                <h2 className="font-bold text-lg">{agent?.firstName || 'Chat'}</h2>
            </div>
            <div className="flex-grow p-4 overflow-y-auto space-y-4">
                {messages.map(msg => (
                    <div key={msg.id} className={`flex items-end gap-2 ${msg.senderId === currentUser?.uid ? 'justify-end' : 'justify-start'}`}>
                        <div className={`py-2 px-4 rounded-2xl max-w-[70%] ${msg.senderId === currentUser?.uid ? 'bg-primary text-white' : 'bg-secondary'}`}>
                            {msg.content}
                        </div>
                    </div>
                ))}
                <div ref={messagesEndRef} />
            </div>
            <form onSubmit={handleSendMessage} className="p-3 border-t flex items-center gap-2">
                <input
                    value={inputMessage}
                    onChange={(e) => setInputMessage(e.target.value)}
                    className="w-full bg-input-background p-3 rounded-lg focus:ring-2 focus:ring-primary focus:outline-none"
                    placeholder="Type your message..."
                />
                <button type="submit" className="bg-primary text-white p-3 rounded-full hover:bg-blue-700 transition-colors disabled:bg-gray-500" disabled={!inputMessage.trim()}>
                    <Send />
                </button>
            </form>
        </div>
    );
};

export default ChatPage;*/
