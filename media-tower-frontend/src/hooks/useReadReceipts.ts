import { useEffect, useRef } from 'react';
import axiosClient from '../api/axiosClient';

interface LiveChatMessage {
    id: string;
    senderId: string;
    isRead: boolean;
}

/**
 * Ce hook détecte s'il y a des messages non lus de l'interlocuteur
 * et envoie une requête pour les marquer tous comme lus une seule fois.
 * @param messages La liste des messages de la conversation.
 * @param chatId L'ID du chat actif.
 * @param currentUserUid L'UID de l'utilisateur actuel (le client).
 */
export const useReadReceipts = (messages: LiveChatMessage[], chatId: string | null, currentUserUid: string | undefined) => {
    // useRef pour s'assurer que l'appel API n'est fait qu'une seule fois par "batch" de messages non lus
    const hasSentReadReceipt = useRef(false);

    useEffect(() => {
        if (!chatId || !currentUserUid || messages.length === 0) {
            return;
        }

        // Vérifier s'il existe au moins un message envoyé par l'autre personne qui n'est pas lu
        const hasUnreadMessages = messages.some(msg => msg.senderId !== currentUserUid && !msg.isRead);

        if (hasUnreadMessages && !hasSentReadReceipt.current) {
            hasSentReadReceipt.current = true; // Bloquer les appels futurs immédiats

            axiosClient.post(`/chats/${chatId}/mark-as-read-by-client`)
                .catch(err => {
                    console.error("Failed to send read receipt, will retry on next message batch.", err);
                    hasSentReadReceipt.current = false; // Permettre une nouvelle tentative en cas d'erreur
                });
        }

        // Réinitialiser le verrou si tous les messages sont lus, pour préparer le prochain message entrant
        if (!hasUnreadMessages) {
            hasSentReadReceipt.current = false;
        }

    }, [messages, chatId, currentUserUid]); // Se déclenche à chaque fois que la liste de messages change
};