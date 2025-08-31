/*
import { useState, useEffect, useRef, FormEvent } from 'react';
import { Minus, X, Send, User, Smile, Paperclip } from 'lucide-react';
import { useChat, Conversation } from '../../contexts/ChatContext';
import { useAuth } from '../../contexts/AuthContext';
import EmojiPicker, { EmojiClickData } from 'emoji-picker-react';

interface ChatWindowProps {
    conversation: Conversation;
    onClose: () => void;
    style?: React.CSSProperties;
}

const ChatWindow = ({ conversation, onClose, style }: ChatWindowProps) => {
    const { sendMessage, onlineAgents } = useChat();
    const { currentUser } = useAuth();
    const [inputMessage, setInputMessage] = useState('');
    const [isMinimized, setIsMinimized] = useState(false);
    const [showEmojiPicker, setShowEmojiPicker] = useState(false);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    const agentIsOnline = onlineAgents.some(a => a.uid === conversation.agent.uid);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [conversation.messages]);

    const handleSendMessage = async (e: FormEvent) => {
        e.preventDefault();
        if (!inputMessage.trim()) return;
        await sendMessage(conversation.id, inputMessage);
        setInputMessage('');
        setShowEmojiPicker(false);
    };

    const onEmojiClick = (emojiData: EmojiClickData) => { setInputMessage(p => p + emojiData.emoji); };

    if (isMinimized) {
        return (
            <div style={style} onClick={() => setIsMinimized(false)} className="fixed bottom-0 w-72 bg-primary text-white p-3 rounded-t-lg shadow-lg cursor-pointer flex justify-between items-center animate-fade-in-up">
                <div className="flex items-center"><User size={18} className="mr-2"/><span className="font-bold">{conversation.agent.firstName}</span></div>
                <X size={18} onClick={(e) => { e.stopPropagation(); onClose(); }}/>
            </div>
        );
    }

    return (
        <div style={style} className="fixed bottom-0 flex flex-col w-80 h-[28rem] bg-card border border-border rounded-t-lg shadow-2xl animate-fade-in-up">
            <header className="bg-primary text-white p-3 flex justify-between items-center rounded-t-lg cursor-pointer" onClick={() => setIsMinimized(true)}>
                <div className="flex items-center">
                    <User size={18} className="mr-2"/>
                    <div>
                        <span className="font-bold">{conversation.agent.firstName}</span>
                        <div className="flex items-center"><span className={`h-2 w-2 rounded-full mr-1.5 ${agentIsOnline ? 'bg-green-400' : 'bg-gray-400'}`}></span><p className="text-xs">{agentIsOnline ? 'Online' : 'Offline'}</p></div>
                    </div>
                </div>
                <div>
                    <Minus size={18} className="mr-2 cursor-pointer hover:bg-white/20 p-1 rounded-full" onClick={(e) => { e.stopPropagation(); setIsMinimized(true); }}/>
                    <X size={18} className="cursor-pointer hover:bg-white/20 p-1 rounded-full" onClick={(e) => { e.stopPropagation(); onClose(); }}/>
                </div>
            </header>
            <main className="flex-grow p-3 overflow-y-auto space-y-3 bg-background">
                {conversation.messages.map(msg => (
                    <div key={msg.id} className={`flex items-end gap-2 text-sm ${msg.senderId === currentUser?.uid ? 'justify-end' : 'justify-start'}`}>
                        <div className={`py-2 px-3 rounded-2xl max-w-[75%] ${msg.senderId === currentUser?.uid ? 'bg-primary text-white rounded-br-none' : 'bg-secondary rounded-bl-none'}`}>{msg.content}</div>
                    </div>
                ))}
                <div ref={messagesEndRef} />
            </main>
            <footer className="relative">
                {showEmojiPicker && ( <div className="absolute bottom-full right-0 z-10"><EmojiPicker onEmojiClick={onEmojiClick}/></div> )}
                <form onSubmit={handleSendMessage} className="p-2 border-t flex items-center gap-2 bg-card">
                    <button type="button" className="p-2 text-gray-500 hover:text-primary"><Paperclip/></button>
                    <input value={inputMessage} onChange={(e) => setInputMessage(e.target.value)} className="w-full bg-input-background p-2 rounded-lg focus:outline-none" placeholder="Aa"/>
                    <button type="button" onClick={() => setShowEmojiPicker(p => !p)} className="p-2 text-gray-500 hover:text-primary"><Smile/></button>
                    <button type="submit" className="bg-primary text-white p-2 rounded-full disabled:bg-gray-500"><Send/></button>
                </form>
            </footer>
        </div>
    );
};

export default ChatWindow;*/
