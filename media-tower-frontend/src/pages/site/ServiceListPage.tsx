// Fichier : src/pages/site/ServiceListPage.tsx (MODIFIÉ)

import { useEffect, useState } from 'react';
import axiosClient from '../../api/axiosClient';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

// --- DÉBUT DES MODIFICATIONS INTERFACES ---
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
    images: Media[]; // On remplace imageUrl par un tableau d'images
}
// --- FIN DES MODIFICATIONS INTERFACES ---

// Composant réutilisable pour afficher une carte de service
const ServiceCard = ({ service }: { service: Service }) => {
    const { i18n } = useTranslation();
    const serviceName = service.names[i18n.language] || service.names.en;
    const serviceDescription = service.descriptions[i18n.language] || service.descriptions.en;

    // --- DÉBUT LOGIQUE D'IMAGE ---
    // On trouve l'image principale ou la première de la liste
    const primaryImage = service.images?.find(img => img.isPrimary) || service.images?.[0];
    const imageUrl = primaryImage ? primaryImage.url : 'https://via.placeholder.com/300';
    // --- FIN LOGIQUE D'IMAGE ---

    return (
        <div className="bg-card rounded-lg overflow-hidden border border-gray-700 group flex flex-col">
            <Link to={`/services/${service.id}`} className="block">
                <img
                    src={imageUrl} // On utilise la nouvelle variable imageUrl
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

// Composant principal de la page
const ServiceListPage = () => {
    const [services, setServices] = useState<Service[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchServices = async () => {
            try {
                // L'appel API reste le même, mais la structure des données reçues a changé
                const response = await axiosClient.get<Service[]>('/services');
                setServices(response.data);
            } catch (error) {
                console.error('Failed to fetch services:', error);
            } finally {
                setLoading(false);
            }
        };
        fetchServices();
    }, []);

    if (loading) return <div className="text-center text-gray-400 p-8">Loading services...</div>;

    return (
        <div>
            <h1 className="text-4xl font-bold mb-8 text-center text-white">Our Services</h1>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {services.map(service => <ServiceCard key={service.id} service={service} />)}
            </div>
        </div>
    );
};

export default ServiceListPage;