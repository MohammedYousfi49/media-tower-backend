import { useEffect, useState } from 'react';
import axiosClient from '../../api/axiosClient';
import { Bell } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import type { Message } from 'stompjs';

interface Notification {
    id: number;
    message: string;
    isRead: boolean;
    createdAt: string;
}

const NotificationDropdown = () => {
    const { stompClient } = useAuth(); // On récupère le client WebSocket du contexte
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [isOpen, setIsOpen] = useState(false);

    const fetchNotifications = () => {
        axiosClient.get<Notification[]>('/notifications')
            .then(res => setNotifications(res.data))
            .catch(err => console.error("Failed to fetch notifications:", err));
    };

    useEffect(() => {
        fetchNotifications();
    }, []);

    useEffect(() => {
        if (stompClient?.connected) {
            const subscription = stompClient.subscribe('/topic/admin/notifications', (message: Message) => {
                console.log('Notification received!', message.body);
                new Audio('/notification.mp3').play().catch(e => console.error("Error playing sound:", e));
                fetchNotifications(); // Recharger la liste pour avoir la nouvelle notification
            });

            return () => {
                subscription.unsubscribe();
            };
        }
    }, [stompClient]);

    const handleMarkAsRead = (id: number) => {
        axiosClient.put(`/notifications/${id}/read`).then(() => {
            setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
        });
    };

    const handleMarkAllAsRead = () => {
        notifications.forEach(n => {
            if(!n.isRead) handleMarkAsRead(n.id);
        });
    };

    const unreadCount = notifications.filter(n => !n.isRead).length;

    return (
        <div className="relative">
            <button onClick={() => setIsOpen(!isOpen)} className="relative text-gray-300 hover:text-white">
                <Bell size={24} />
                {unreadCount > 0 && (
                    <span className="absolute -top-1 -right-1 flex h-4 w-4">
                        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
                        <span className="relative inline-flex rounded-full h-4 w-4 bg-red-500 text-xs items-center justify-center">{unreadCount}</span>
                    </span>
                )}
            </button>

            {isOpen && (
                <div className="absolute right-0 mt-2 w-80 bg-card border border-gray-700 rounded-md shadow-lg z-20">
                    <div className="p-2 flex justify-between items-center font-bold border-b border-gray-700">
                        <span>Notifications</span>
                        {unreadCount > 0 && <button onClick={handleMarkAllAsRead} className="text-xs font-normal text-blue-400 hover:underline">Mark all as read</button>}
                    </div>
                    <ul className="max-h-96 overflow-y-auto">
                        {notifications.length > 0 ? notifications.map(notif => (
                            <li key={notif.id} className={`p-2 border-b border-gray-700 text-sm ${!notif.isRead ? 'bg-blue-900/50' : ''}`}>
                                <p>{notif.message}</p>
                                <div className="flex justify-between items-center mt-1">
                                    <span className="text-xs text-gray-500">{new Date(notif.createdAt).toLocaleString()}</span>
                                    {!notif.isRead && (
                                        <button onClick={() => handleMarkAsRead(notif.id)} className="text-xs text-blue-400 hover:underline">Mark as read</button>
                                    )}
                                </div>
                            </li>
                        )) : <li className="p-4 text-center text-gray-500">No new notifications</li>}
                    </ul>
                </div>
            )}
        </div>
    );
};

export default NotificationDropdown;