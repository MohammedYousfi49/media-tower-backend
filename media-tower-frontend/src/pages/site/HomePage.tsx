// Fichier : src/pages/site/HomePage.tsx (Corrigé)

import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axiosClient from '../../api/axiosClient';
import ProductCard from '../../components/site/ProductCard';
import ServiceCard from '../../components/site/ServiceCard'; // Cet import est maintenant correct

// Interfaces
interface Media {
    id: number;
    url: string;
    isPrimary: boolean;
}

interface Product {
    id: number;
    names: { [key: string]: string };
    price: number;
    images: Media[];
    categoryName: string;
}

// --- Début de la correction de l'interface Service ---
interface Service {
    id: number;
    names: { [key: string]: string };
    descriptions: { [key: string]: string };
    price: number;
    images: Media[]; // On utilise la nouvelle structure
}
// --- Fin de la correction de l'interface Service ---

const HomePage = () => {
    const [popularProducts, setPopularProducts] = useState<Product[]>([]);
    const [popularServices, setPopularServices] = useState<Service[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const [productsRes, servicesRes] = await Promise.all([
                    axiosClient.get<Product[]>('/products/popular'),
                    axiosClient.get<Service[]>('/services'), // L'appel reste le même, mais la structure attendue change
                ]);
                setPopularProducts(productsRes.data);
                setPopularServices(servicesRes.data.slice(0, 3));
            } catch (error) {
                console.error("Failed to fetch homepage data:", error);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    return (
        <div className="space-y-16 md:space-y-24">
            <section className="text-center bg-card border border-border rounded-lg p-8 md:p-12 animate-fade-in">
                <h1 className="text-4xl md:text-5xl font-extrabold text-white mb-4 tracking-tight">Welcome to Media Tower</h1>
                <p className="text-lg md:text-xl text-foreground max-w-2xl mx-auto mb-8">
                    Discover a world of premium digital products and expert services to boost your project.
                </p>
                <Link to="/products" className="bg-primary text-primary-foreground font-bold px-8 py-3 rounded-lg hover:bg-opacity-80 transition-transform hover:scale-105 inline-block">
                    Browse All Offers
                </Link>
            </section>

            <section>
                <h2 className="text-3xl font-bold text-center mb-8 text-white">Popular Products</h2>
                {loading ? <p className="text-center text-foreground">Loading...</p> : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                        {popularProducts.map(product => (
                            <ProductCard key={product.id} product={product} />
                        ))}
                    </div>
                )}
            </section>

            <section>
                <h2 className="text-3xl font-bold text-center mb-8 text-white">Featured Services</h2>
                {loading ? <p className="text-center text-foreground">Loading...</p> : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                        {popularServices.map(service => <ServiceCard key={service.id} service={service} />)}
                    </div>
                )}
            </section>
        </div>
    );
};

export default HomePage;