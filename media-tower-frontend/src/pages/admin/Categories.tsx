import { useEffect, useState } from 'react';
import { PlusCircle, Edit, Trash2 } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Modal from '../../components/shared/Modal';

// Définir un type pour la catégorie pour plus de robustesse
interface Category {
    id: number;
    names: { [key: string]: string };
    descriptions: { [key: string]: string };
}

const Categories = () => {
    const [categories, setCategories] = useState<Category[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [currentCategory, setCurrentCategory] = useState<Category | null>(null);
    const [formData, setFormData] = useState({
        names: { fr: '', en: '', ar: '' },
        descriptions: { fr: '', en: '', ar: '' }
    });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchCategories();
    }, []);

    const fetchCategories = async () => {
        setLoading(true);
        try {
            const response = await axiosClient.get('/categories');
            setCategories(response.data);
        } catch (error) {
            console.error('Failed to fetch categories:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleOpenModal = (category: Category | null = null) => {
        setCurrentCategory(category);
        if (category) {
            // S'assurer que tous les champs existent pour éviter les erreurs "undefined"
            setFormData({
                names: { fr: category.names.fr || '', en: category.names.en || '', ar: category.names.ar || '' },
                descriptions: { fr: category.descriptions.fr || '', en: category.descriptions.en || '', ar: category.descriptions.ar || '' },
            });
        } else {
            // Réinitialiser le formulaire pour une nouvelle catégorie
            setFormData({
                names: { fr: '', en: '', ar: '' },
                descriptions: { fr: '', en: '', ar: '' }
            });
        }
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setCurrentCategory(null);
    };

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            if (currentCategory) {
                await axiosClient.put(`/categories/${currentCategory.id}`, formData);
            } else {
                await axiosClient.post('/categories', formData);
            }
            fetchCategories(); // Rafraîchir la liste après sauvegarde
            handleCloseModal();
        } catch (error) {
            console.error('Failed to save category:', error);
            // Ici, vous pourriez afficher une notification d'erreur à l'utilisateur
        }
    };

    const handleDelete = async (id: number) => {
        if (window.confirm('Are you sure you want to delete this category?')) {
            try {
                await axiosClient.delete(`/categories/${id}`);
                fetchCategories(); // Rafraîchir la liste après suppression
            } catch (error) {
                console.error('Failed to delete category:', error);
            }
        }
    };

    const handleInputChange = (lang: string, field: 'names' | 'descriptions', value: string) => {
        setFormData(prev => ({
            ...prev,
            [field]: { ...prev[field], [lang]: value }
        }));
    };

    return (
        <div>
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-3xl font-bold text-white">Manage Categories</h1>
                <button
                    onClick={() => handleOpenModal()}
                    className="bg-primary text-white px-4 py-2 rounded-lg flex items-center hover:bg-blue-600 transition-colors"
                >
                    <PlusCircle className="mr-2" size={20} /> Add New
                </button>
            </div>

            <div className="bg-card p-4 rounded-lg border border-gray-700">
                <table className="w-full text-left">
                    <thead>
                    <tr className="border-b border-gray-600">
                        <th className="p-4">Name (FR)</th>
                        <th className="p-4">Name (EN)</th>
                        <th className="p-4">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {loading ? (
                        <tr><td colSpan={3} className="text-center p-4">Loading...</td></tr>
                    ) : categories.map((cat) => (
                        <tr key={cat.id} className="border-b border-gray-700 hover:bg-gray-800">
                            <td className="p-4">{cat.names.fr || '-'}</td>
                            <td className="p-4">{cat.names.en || '-'}</td>
                            <td className="p-4 flex space-x-2">
                                <button onClick={() => handleOpenModal(cat)} className="text-yellow-400 hover:text-yellow-300"><Edit size={20} /></button>
                                <button onClick={() => handleDelete(cat.id)} className="text-red-500 hover:text-red-400"><Trash2 size={20} /></button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            <Modal isOpen={isModalOpen} onClose={handleCloseModal} title={currentCategory ? 'Edit Category' : 'Add New Category'}>
                <form onSubmit={handleSave}>
                    {['fr', 'en', 'ar'].map(lang => (
                        <div key={lang} className="mb-4">
                            <h3 className="font-bold mb-2 text-white">Language: {lang.toUpperCase()}</h3>
                            <div className="mb-2">
                                <label className="block text-gray-400 mb-1">Name ({lang})</label>
                                <input
                                    type="text"
                                    value={formData.names[lang as keyof typeof formData.names]}
                                    onChange={(e) => handleInputChange(lang, 'names', e.target.value)}
                                    className="w-full bg-gray-700 text-white p-2 rounded-md border border-gray-600"
                                    required={lang === 'fr' || lang === 'en'} // Rendre au moins FR et EN obligatoires
                                />
                            </div>
                            <div>
                                <label className="block text-gray-400 mb-1">Description ({lang})</label>
                                <textarea
                                    value={formData.descriptions[lang as keyof typeof formData.descriptions]}
                                    onChange={(e) => handleInputChange(lang, 'descriptions', e.target.value)}
                                    className="w-full bg-gray-700 text-white p-2 rounded-md border border-gray-600"
                                    rows={3}
                                />
                            </div>
                        </div>
                    ))}
                    <div className="flex justify-end space-x-4 mt-6 border-t border-gray-700 pt-4">
                        <button type="button" onClick={handleCloseModal} className="bg-gray-600 text-white px-4 py-2 rounded-lg hover:bg-gray-500 transition-colors">Cancel</button>
                        <button type="submit" className="bg-primary text-white px-4 py-2 rounded-lg hover:bg-blue-600 transition-colors">Save</button>
                    </div>
                </form>
            </Modal>
        </div>
    );
};

export default Categories;