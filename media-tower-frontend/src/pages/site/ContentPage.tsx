import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import axiosClient from '../../api/axiosClient';
import { useTranslation } from 'react-i18next';

interface Content {
    id: number;
    slug: string;
    titles: { [key: string]: string };
    bodies: { [key: string]: string };
}

const ContentPage = () => {
    const { slug } = useParams<{ slug: string }>();
    const { i18n } = useTranslation();
    const [content, setContent] = useState<Content | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!slug) return;

        const fetchContent = async () => {
            setLoading(true);
            try {
                const response = await axiosClient.get<Content>(`/content/${slug}`);
                setContent(response.data);
            } catch (error) {
                console.error(`Failed to fetch content for slug: ${slug}`, error);
                setContent(null); // Gérer le cas où la page n'est pas trouvée
            } finally {
                setLoading(false);
            }
        };
        fetchContent();
    }, [slug]);

    if (loading) {
        return <div className="text-center text-gray-400 p-8">Loading page...</div>;
    }

    if (!content) {
        return <div className="text-center text-gray-400 p-8">Page not found.</div>;
    }

    const title = content.titles[i18n.language] || content.titles.en;
    const body = content.bodies[i18n.language] || content.bodies.en;

    return (
        <div className="bg-card border border-gray-700 rounded-lg p-6 md:p-8 max-w-4xl mx-auto">
            <h1 className="text-3xl md:text-4xl font-bold text-white mb-6">{title}</h1>
            {/* Pour afficher le HTML de manière sécurisée, il faudrait une librairie comme DOMPurify.
                Pour notre cas, on fait confiance au contenu venant de notre admin panel. */}
            <div
                className="prose prose-invert max-w-none text-gray-300 prose-headings:text-white"
                dangerouslySetInnerHTML={{ __html: body.replace(/\n/g, '<br />') }}
            />
        </div>
    );
};

export default ContentPage;