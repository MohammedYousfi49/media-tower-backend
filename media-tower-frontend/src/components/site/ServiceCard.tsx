// Fichier : src/components/site/ServiceCard.tsx (Corrigé et mis à jour)

import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

// --- Début des modifications ---
interface Media {
    id: number;
    url: string;
    isPrimary: boolean;
}

interface Service {
    id: number;
    names: { [key: string]: string };
    descriptions: { [key: string]: string };
    price: number;
    images: Media[]; // On utilise la nouvelle structure
}
// --- Fin des modifications ---

const ServiceCard = ({ service }: { service: Service }) => {
    const { i18n } = useTranslation();
    const serviceName = service.names[i18n.language] || service.names.en;
    const serviceDescription = service.descriptions[i18n.language] || service.descriptions.en;

    // --- Logique pour trouver l'URL de l'image ---
    const primaryImage = service.images?.find(img => img.isPrimary) || service.images?.[0];
    const imageUrl = primaryImage ? primaryImage.url : 'https://via.placeholder.com/300';

    return (
        <div className="bg-card rounded-lg overflow-hidden border border-gray-700 group flex flex-col">
            <Link to={`/services/${service.id}`} className="block">
                <img
                    src={imageUrl} // On utilise la nouvelle variable
                    alt={serviceName}
                    className="w-full h-48 object-cover group-hover:opacity-80 transition-opacity"
                />
            </Link>
            <div className="p-4 flex-grow flex flex-col">
                <h3 className="text-lg font-bold text-white mt-1 flex-grow">
                    <Link to={`/services/${service.id}`} className="hover:text-primary">{serviceName}</Link>
                </h3>
                <p className="text-sm text-gray-400 mt-2 flex-grow">
                    {serviceDescription.substring(0, 100)}...
                </p>
                <div className="mt-4 flex justify-between items-center">
                    <p className="text-xl font-semibold text-primary">{service.price.toFixed(2)} DH</p>
                    <Link to={`/services/${service.id}`} className="bg-primary text-white font-bold px-4 py-2 rounded-lg hover:bg-blue-600 text-sm">
                        Réserver
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default ServiceCard;