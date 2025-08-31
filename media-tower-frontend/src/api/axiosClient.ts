import axios from 'axios';
import { auth } from '../lib/firebase';

const axiosClient = axios.create({
    baseURL: 'http://localhost:8080/api',
});

// NOUVELLE LOGIQUE : On crée une fonction pour attacher le token
// que l'on appellera manuellement avant les requêtes importantes.
export const getAuthenticatedClient = async () => {
    const user = auth.currentUser;
    if (user) {
        try {
            const token = await user.getIdToken();
            axiosClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        } catch (error) {
            console.error("Erreur lors de la récupération du token d'authentification :", error);
            delete axiosClient.defaults.headers.common['Authorization'];
        }
    } else {
        delete axiosClient.defaults.headers.common['Authorization'];
    }
    return axiosClient;
};

// Intercepteur de base pour les requêtes qui ne nécessitent pas cette garantie.
axiosClient.interceptors.request.use(
    async (config) => {
        if (!config.headers.Authorization) {
            const user = auth.currentUser;
            if (user) {
                try {
                    const token = await user.getIdToken();
                    config.headers.Authorization = `Bearer ${token}`;
                } catch (error) {
                    console.error("L'intercepteur a échoué à récupérer le token :", error);
                }
            }
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default axiosClient;