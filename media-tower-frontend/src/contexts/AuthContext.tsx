/* eslint-disable react-refresh/only-export-components */
import { createContext, useState, useEffect, useContext, ReactNode } from 'react';
import type { User } from 'firebase/auth';
import { onAuthStateChanged } from 'firebase/auth';
import { Client, over } from 'stompjs';
import SockJS from 'sockjs-client';
import { auth } from '../lib/firebase';
import axiosClient from '../api/axiosClient';

let stompConnection: Client | null = null;

export interface AppUser {
  id: number;
  uid: string;
  email: string;
  firstName: string;
  lastName: string;
  role: 'USER' | 'ADMIN' | 'SELLER';
  status: 'ACTIVE' | 'BLOCKED';
}

interface AuthContextType {
  currentUser: User | null;
  appUser: AppUser | null;
  loading: boolean;
  stompClient: Client | null;
  setAppUser: (user: AppUser | null) => void;
}

export const AuthContext = createContext<AuthContextType>({
  currentUser: null,
  appUser: null,
  loading: true,
  stompClient: null,
  setAppUser: () => {},
});

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [appUser, setAppUser] = useState<AppUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [stompClient, setStompClient] = useState<Client | null>(null);

  useEffect(() => {
    const connectWebSocket = async (user: User) => {
      console.log("AuthContext: connectWebSocket - Vérification de la connexion existante.");
      if (stompConnection?.connected) {
        console.log("AuthContext: connectWebSocket - Connexion déjà active.");
        setStompClient(stompConnection);
        return;
      }

      try {
        console.log("AuthContext: connectWebSocket - Création d'une nouvelle connexion WebSocket.");
        const token = await user.getIdToken(true);
        const socket = new SockJS('http://localhost:8080/ws');
        const client = over(socket);

        // Configuration d'un timeout de connexion
        const connectPromise = new Promise((resolve, reject) => {
          const timer = setTimeout(() => {
            reject(new Error('WebSocket connection timeout'));
          }, 10000); // 10 secondes timeout

          client.connect(
              { Authorization: `Bearer ${token}` },
              () => {
                clearTimeout(timer);
                console.log('%cAuthContext: CONNEXION WEBSOCKET RÉUSSIE !', 'color: #28a745; font-weight: bold;');
                stompConnection = client;
                setStompClient(client);
                resolve(client);
              },
              (error) => {
                clearTimeout(timer);
                console.error('%cAuthContext: ERREUR LORS DE LA CONNEXION WEBSOCKET.', 'color: #dc3545; font-weight: bold;', error);
                reject(error);
              }
          );
        });

        await connectPromise;
      } catch (error) {
        console.error("AuthContext: Erreur lors de la connexion WebSocket:", error);
        // Réessayer après 5 secondes
        setTimeout(() => {
          if (user && !stompConnection?.connected) {
            connectWebSocket(user);
          }
        }, 5000);
      }
    };

    const disconnectWebSocket = () => {
      console.log("AuthContext: disconnectWebSocket - Tentative de déconnexion.");
      if (stompConnection?.connected) {
        stompConnection.disconnect(() => {
          console.log('%cAuthContext: WEBSOCKET DÉCONNECTÉ.', 'color: #ffc107; font-weight: bold;');
          stompConnection = null;
          setStompClient(null);
        });
      } else {
        console.log("AuthContext: disconnectWebSocket - Aucune connexion active à déconnecter.");
      }
    };

    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      console.log("AuthContext: onAuthStateChanged - Statut de l'utilisateur a changé. User:", user ? user.uid : null);
      setCurrentUser(user);
      if (user) {
        if (!appUser || appUser.uid !== user.uid) {
          setLoading(true);
          try {
            const token = await user.getIdToken();
            axiosClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
            const response = await axiosClient.get<AppUser>('/users/me');
            console.log("AuthContext: onAuthStateChanged - Données de l'utilisateur récupérées depuis /users/me.", response.data);
            setAppUser(response.data);
            await connectWebSocket(user);
          } catch (error) {
            console.error("AuthContext: Échec de la récupération des données de l'utilisateur, déconnexion.", error);
            await auth.signOut();
            setAppUser(null);
          } finally {
            setLoading(false);
          }
        } else {
          setLoading(false);
        }
      } else {
        delete axiosClient.defaults.headers.common['Authorization'];
        setAppUser(null);
        disconnectWebSocket();
        setLoading(false);
      }
    });

    return () => unsubscribe();
  }, []); // <-- Modification demandée ici (suppression des dépendances currentUser et appUser)

  const value = { currentUser, appUser, loading, stompClient, setAppUser };

  return <AuthContext.Provider value={value}>{!loading && children}</AuthContext.Provider>;
};

export const AuthProviderWrapper = ({ children }: { children: ReactNode }) => {
  return <AuthProvider>{children}</AuthProvider>;
};