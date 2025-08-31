import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

const AuthRedirect = () => {
    const { appUser, loading } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        // On attend que le chargement des données de l'utilisateur soit terminé
        if (!loading && appUser) {
            // La logique de redirection
            if (appUser.role === 'ADMIN' || appUser.role === 'SELLER') {
                navigate('/admin/dashboard', { replace: true });
            } else {
                navigate('/products', { replace: true }); // Redirige l'utilisateur normal vers la boutique
            }
        }
    }, [appUser, loading, navigate]);

    // Pendant le chargement, on affiche un message
    return <div className="flex justify-center items-center h-screen bg-background text-foreground">Authenticating...</div>;
};

export default AuthRedirect;