/*
import { useChat } from '../../contexts/ChatContext';
import ChatWindow from './ChatWindow';

const ChatWindowManager = () => {
    const { activeChatWindows, closeChatWindow } = useChat();

    const windowWidth = 320;
    const windowOffset = 16;
    const bubbleOffset = 96;

    return (
        <>
            {activeChatWindows.map((convo, index) => (
                <ChatWindow
                    key={convo.id}
                    conversation={convo}
                    onClose={() => closeChatWindow(convo.id)}
                    style={{ right: `${bubbleOffset + (index * (windowWidth + windowOffset))}px` }}
                />
            ))}
        </>
    );
};

export default ChatWindowManager;*/
