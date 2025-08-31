// src/contexts/AuthProvider.tsx (Code corrigé)

import { useState, useEffect, ReactNode } from 'react';
import { onAuthStateChanged, User } from 'firebase/auth';
import { Client, over } from 'stompjs';
import SockJS from 'sockjs-client';
import { auth } from '../lib/firebase';
import axiosClient from '../api/axiosClient';

import { AuthContext, AppUser } from './AuthContext';

let stompConnection: Client | null = null;

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [currentUser, setCurrentUser] = useState<User | null>(null);
    const [appUser, setAppUser] = useState<AppUser | null>(null);
    const [loading, setLoading] = useState(true);
    const [stompClient, setStompClient] = useState<Client | null>(null);

    useEffect(() => {
        const connectWebSocket = async (user: User) => {
            if (stompConnection?.connected) {
                setStompClient(stompConnection);
                return;
            }
            try {
                const token = await user.getIdToken(true);
                const socket = new SockJS('http://localhost:8080/ws');
                const client = over(socket);
                client.debug = () => {};
                client.connect({ Authorization: `Bearer ${token}` }, () => {
                    stompConnection = client;
                    setStompClient(client);
                });
            } catch (error) {
                console.error("WebSocket Auth Error:", error);
            }
        };

        const disconnectWebSocket = () => {
            if (stompConnection?.connected) {
                stompConnection.disconnect(() => {
                    stompConnection = null;
                    setStompClient(null);
                });
            }
        };

        const unsubscribe = onAuthStateChanged(auth, async (user) => {
            setCurrentUser(user);
            if (user) {
                try {
                    // Mettre à jour l'en-tête d'autorisation du client Axios
                    const token = await user.getIdToken();
                    axiosClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;

                    // Effectuer la requête pour obtenir les données de l'utilisateur
                    const response = await axiosClient.get<AppUser>('/users/me');
                    setAppUser(response.data);
                    await connectWebSocket(user);
                } catch (error) {
                    console.error("Failed to fetch app user data, signing out.", error);
                    await auth.signOut();
                    setAppUser(null);
                }
            } else {
                setAppUser(null);
                disconnectWebSocket();
                // Assurez-vous de supprimer l'en-tête d'autorisation lors de la déconnexion
                delete axiosClient.defaults.headers.common['Authorization'];
            }
            setLoading(false);
        });

        return () => unsubscribe();
    }, []);

    const value = { currentUser, appUser, loading, stompClient, setAppUser };

    return <AuthContext.Provider value={value}>{!loading && children}</AuthContext.Provider>;
};