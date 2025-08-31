import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

// Définir les props pour le composant
interface ProtectedRouteProps {
  allowedRoles: string[];
  children: ReactNode;
}

const ProtectedRoute = ({ allowedRoles, children }: ProtectedRouteProps) => {
  const { currentUser, appUser, loading } = useAuth();

  if (loading) {
    return <div className="flex justify-center items-center h-screen">Loading...</div>;
  }

  if (!currentUser) {
    return <Navigate to="/login" replace />;
  }

  // Si appUser n'est pas encore chargé, on peut attendre aussi
  if (!appUser) {
    return <div className="flex justify-center items-center h-screen">Loading user data...</div>;
  }

  const userHasRequiredRole = allowedRoles.includes(appUser?.role);

  if (userHasRequiredRole) {
    // Si l'utilisateur a le bon rôle, on affiche les enfants (le layout et son contenu)
    return <>{children}</>;
  } else {
    // Redirige vers une page "non autorisé" ou la page d'accueil
    return <Navigate to="/" replace />;
  }
};

export default ProtectedRoute;