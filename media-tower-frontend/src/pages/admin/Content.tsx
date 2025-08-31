import { useEffect, useState } from 'react';
import { Edit } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Modal from '../../components/shared/Modal';
import { AxiosError } from 'axios';

interface Content {
    id: number;
    slug: string;
    titles: { [key: string]: string };
    bodies: { [key: string]: string };
}

const Content = () => {
    const [contents, setContents] = useState<Content[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [currentContent, setCurrentContent] = useState<Content | null>(null);
    const [formData, setFormData] = useState({
        titles: { fr: '', en: '', ar: '' },
        bodies: { fr: '', en: '', ar: '' }
    });
    const [loading, setLoading] = useState(true);
    const [apiError, setApiError] = useState('');

    useEffect(() => {
        fetchContent();
    }, []);

    const fetchContent = async () => {
        setLoading(true);
        try {
            const response = await axiosClient.get('/content');
            setContents(response.data);
        } catch (error) {
            console.error('Failed to fetch content:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleOpenModal = (content: Content) => {
        setCurrentContent(content);
        setApiError('');
        setFormData({
            titles: { fr: content.titles.fr || '', en: content.titles.en || '', ar: content.titles.ar || '' },
            bodies: { fr: content.bodies.fr || '', en: content.bodies.en || '', ar: content.bodies.ar || '' },
        });
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setCurrentContent(null);
    };

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        setApiError('');
        if (!currentContent) return;

        try {
            await axiosClient.put(`/content/${currentContent.id}`, {
                slug: currentContent.slug, // Le slug n'est pas modifiable
                titles: formData.titles,
                bodies: formData.bodies,
            });
            fetchContent();
            handleCloseModal();
        } catch (error) {
            console.error('Failed to save content:', error);
            if (error instanceof AxiosError && error.response) {
                const responseData = error.response.data as { message?: string } | string;
                if (typeof responseData === 'string') {
                    setApiError(responseData);
                } else {
                    setApiError(responseData.message || 'Failed to save content.');
                }
            } else {
                setApiError('An unexpected error occurred.');
            }
        }
    };

    const handleInputChange = (lang: string, field: 'titles' | 'bodies', value: string) => {
        setFormData(prev => ({
            ...prev,
            [field]: { ...prev[field], [lang]: value }
        }));
    };

    return (
        <div>
            <h1 className="text-3xl font-bold text-white mb-6">Manage Site Content</h1>

            <div className="bg-card p-4 rounded-lg border border-gray-700">
                <table className="w-full text-left">
                    <thead>
                    <tr className="border-b border-gray-600">
                        <th className="p-4">Page Title (EN)</th>
                        <th className="p-4">Identifier (Slug)</th>
                        <th className="p-4">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {loading ? (
                        <tr><td colSpan={3} className="text-center p-4">Loading...</td></tr>
                    ) : contents.map((content) => (
                        <tr key={content.id} className="border-b border-gray-700 hover:bg-gray-800">
                            <td className="p-4 font-medium">{content.titles.en || '-'}</td>
                            <td className="p-4 text-gray-400 font-mono">{content.slug}</td>
                            <td className="p-4">
                                <button onClick={() => handleOpenModal(content)} className="text-yellow-400 hover:text-yellow-300"><Edit size={20} /></button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            {currentContent && (
                <Modal isOpen={isModalOpen} onClose={handleCloseModal} title={`Edit Content: ${currentContent.titles.en}`}>
                    <form onSubmit={handleSave}>
                        {['fr', 'en', 'ar'].map(lang => (
                            <div key={lang} className="mb-4">
                                <h3 className="font-bold mb-2 text-white">Language: {lang.toUpperCase()}</h3>
                                <div className="mb-2">
                                    <label className="block text-gray-400 mb-1">Title ({lang})</label>
                                    <input
                                        type="text"
                                        value={formData.titles[lang as keyof typeof formData.titles]}
                                        onChange={(e) => handleInputChange(lang, 'titles', e.target.value)}
                                        className="w-full bg-gray-700 text-white p-2 rounded-md border border-gray-600"
                                    />
                                </div>
                                <div>
                                    <label className="block text-gray-400 mb-1">Body ({lang})</label>
                                    <textarea
                                        value={formData.bodies[lang as keyof typeof formData.bodies]}
                                        onChange={(e) => handleInputChange(lang, 'bodies', e.target.value)}
                                        className="w-full bg-gray-700 text-white p-2 rounded-md border border-gray-600"
                                        rows={8}
                                    />
                                </div>
                            </div>
                        ))}
                        {apiError && <div className="p-3 my-4 text-center text-red-400 bg-red-900/20 border border-red-500/50 rounded-lg">{apiError}</div>}
                        <div className="flex justify-end space-x-4 mt-6 border-t border-gray-700 pt-4">
                            <button type="button" onClick={handleCloseModal} className="bg-gray-600 text-white px-4 py-2 rounded-lg">Cancel</button>
                            <button type="submit" className="bg-primary text-white px-4 py-2 rounded-lg">Save Changes</button>
                        </div>
                    </form>
                </Modal>
            )}
        </div>
    );
};

export default Content;