// Fichier : src/pages/admin/Packs.tsx (NETTOYÉ ET FINAL)

import { useEffect, useState, FormEvent, ChangeEvent } from 'react';
// --- CORRECTION : Suppression des imports inutilisés ---
import { PlusCircle, Edit, Trash2, Loader2, Package, XCircle } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Modal from '../../components/shared/Modal';
import Select from 'react-select';

// --- Interfaces ---
interface Media { id: number; url: string; originalName: string; isPrimary: boolean; file?: File; isNew?: boolean; isDeleted?: boolean; }
interface ProductPack { id: number; names: { [key: string]: string }; descriptions: { [key: string]: string }; price: number; images: Media[]; productIds: number[]; }
interface ProductForSelect { value: number; label: string; }
// --- CORRECTION : Interface pour typer la réponse de l'API Products ---
interface ProductApiResponse { id: number; names: { [key: string]: string }; }

const LANGUAGES: Array<'fr' | 'en'> = ['fr', 'en'];

const Packs = () => {
    const [packs, setPacks] = useState<ProductPack[]>([]);
    const [productsForSelect, setProductsForSelect] = useState<ProductForSelect[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [currentPack, setCurrentPack] = useState<Partial<ProductPack>>({});
    const [saving, setSaving] = useState(false);
    const [loading, setLoading] = useState(true);

    const fetchData = async () => {
        setLoading(true);
        try {
            const [packsRes, productsRes] = await Promise.all([
                axiosClient.get<ProductPack[]>('/packs'),
                axiosClient.get<ProductApiResponse[]>('/products') // --- CORRECTION : Utilisation du type
            ]);
            setPacks(packsRes.data);
            const productOptions = productsRes.data.map(p => ({ value: p.id, label: p.names.en || `Product ${p.id}` }));
            setProductsForSelect(productOptions);
        } catch (error) {
            console.error('Failed to fetch initial data:', error);
            alert('Failed to load data.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void fetchData();
    }, []);

    const handleOpenModal = (pack: Partial<ProductPack> | null = null) => {
        const initialPack: Partial<ProductPack> = { names: { fr: '', en: '' }, descriptions: { fr: '', en: '' }, price: 0, images: [], productIds: [] };
        setCurrentPack({
            ...initialPack,
            ...pack,
            names: { ...initialPack.names, ...pack?.names },
            descriptions: { ...initialPack.descriptions, ...pack?.descriptions },
            images: pack?.images ? [...pack.images] : [],
            productIds: pack?.productIds ? [...pack.productIds] : [],
        });
        setIsModalOpen(true);
    };

    const handleCloseModal = () => setIsModalOpen(false);

    const handleSave = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if ((currentPack.productIds || []).length === 0) {
            alert('A pack must contain at least one product.');
            return;
        }
        setSaving(true);
        const form = e.currentTarget;

        const packDto = {
            id: currentPack.id,
            names: { fr: (form.name_fr as HTMLInputElement).value, en: (form.name_en as HTMLInputElement).value },
            descriptions: { fr: (form.desc_fr as HTMLTextAreaElement).value, en: (form.desc_en as HTMLTextAreaElement).value },
            price: Number((form.price as HTMLInputElement).value),
            productIds: currentPack.productIds,
        };

        const formData = new FormData();
        formData.append("packDto", new Blob([JSON.stringify(packDto)], { type: "application/json" }));

        currentPack.images?.filter(img => img.isNew && img.file).forEach(media => {
            if (media.file) formData.append("newImages", media.file);
        });

        try {
            if (currentPack?.id) {
                await axiosClient.put(`/packs/${currentPack.id}`, formData);
            } else {
                await axiosClient.post('/packs', formData);
            }
            await fetchData();
            handleCloseModal();
        } catch (error) {
            console.error('Failed to save pack', error);
            alert('Error saving pack. Check console for details.');
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (id: number) => {
        if (window.confirm('Are you sure you want to delete this pack?')) {
            try {
                await axiosClient.delete(`/packs/${id}`);
                setPacks(prev => prev.filter(p => p.id !== id));
                alert('Pack deleted successfully!');
            } catch (error) {
                console.error('Failed to delete pack', error);
                alert('Error deleting pack.');
            }
        }
    };

    const handleImageFileChange = (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files) {
            const files = Array.from(e.target.files);
            const newMediaFiles: Media[] = files.map(file => ({
                id: Date.now() + Math.random(),
                url: URL.createObjectURL(file),
                originalName: file.name,
                isPrimary: false,
                file: file,
                isNew: true,
            }));

            setCurrentPack(prev => {
                if (!prev) return prev;
                const currentImages = prev.images?.filter(img => !img.isDeleted) || [];
                const allImages = [...currentImages, ...newMediaFiles];
                if (allImages.length > 0 && !allImages.some(img => img.isPrimary)) {
                    allImages[0].isPrimary = true;
                }
                return { ...prev, images: allImages };
            });
        }
    };

    const handleSetPrimaryImage = (id: number) => {
        setCurrentPack(prev => {
            if (!prev) return prev;
            return {
                ...prev,
                images: prev.images?.map(img => ({ ...img, isPrimary: img.id === id })) || []
            }
        });
    };

    const handleRemoveMedia = (id: number) => {
        setCurrentPack(prev => {
            if (!prev) return prev;
            const mediaList = prev.images || [];
            const updatedList = mediaList.map(media =>
                media.id === id ? { ...media, isDeleted: true } : media
            ).filter(media => !(media.isNew && media.isDeleted));

            if (prev.images?.find(img => img.id === id)?.isPrimary) {
                const remainingImages = updatedList.filter(img => !img.isDeleted);
                if (remainingImages.length > 0) {
                    remainingImages[0].isPrimary = true;
                }
            }
            return { ...prev, images: updatedList };
        });
    };

    return (
        <div className="p-6 bg-background min-h-screen text-text-color">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-3xl font-bold text-white">Manage Packs</h1>
                <button
                    onClick={() => handleOpenModal()}
                    className="bg-primary text-primary-foreground px-4 py-2 rounded-lg flex items-center hover:bg-primary/90 transition-colors shadow-md"
                >
                    <PlusCircle className="mr-2" size={20} /> Add New Pack
                </button>
            </div>

            <div className="bg-card p-4 rounded-lg border border-border overflow-x-auto shadow-lg">
                <table className="w-full text-left table-auto">
                    <thead>
                    <tr className="border-b border-border text-sm text-gray-400 uppercase">
                        <th className="p-3 w-16">Image</th>
                        <th className="p-3">Name (EN)</th>
                        <th className="p-3">Products</th>
                        <th className="p-3 w-28 text-right">Price (DH)</th>
                        <th className="p-3 w-24 text-center">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {loading ? (
                        <tr><td colSpan={5} className="text-center p-8"><Loader2 className="animate-spin inline-block mr-2 text-primary" size={24} /> Loading packs...</td></tr>
                    ) : packs.map(pack => {
                        const primaryImage = pack.images?.find(img => img.isPrimary) || pack.images?.[0];
                        return (
                            <tr key={pack.id} className="border-b border-border last:border-b-0 hover:bg-hover-card transition-colors">
                                <td className="p-2">
                                    <img src={primaryImage?.url || 'https://via.placeholder.com/40x40?text=No+Img'} alt={pack.names.en} className="w-12 h-12 rounded object-cover border border-gray-700" />
                                </td>
                                <td className="p-3">{pack.names.en}</td>
                                <td className="p-3">
                                        <span className="inline-flex items-center gap-1 bg-gray-700 text-white text-xs px-2 py-1 rounded-full">
                                            <Package size={14} /> {pack.productIds?.length || 0} Products
                                        </span>
                                </td>
                                <td className="p-3 text-right">{pack.price.toFixed(2)}</td>
                                <td className="flex space-x-2 p-3 justify-center items-center h-full">
                                    <button onClick={() => handleOpenModal(pack)} className="text-yellow-400 hover:text-yellow-300"><Edit size={18} /></button>
                                    <button onClick={() => handleDelete(pack.id)} className="text-red-500 hover:text-red-400"><Trash2 size={18} /></button>
                                </td>
                            </tr>
                        );
                    })}
                    </tbody>
                </table>
            </div>

            {isModalOpen && currentPack && (
                <Modal isOpen={isModalOpen} onClose={handleCloseModal} title={currentPack.id ? 'Edit Pack' : 'Add New Pack'}>
                    <form onSubmit={handleSave} className="space-y-6 max-h-[80vh] overflow-y-auto p-2 scrollbar-thin scrollbar-thumb-gray-600 scrollbar-track-gray-800">

                        <div className="border border-border p-4 rounded-md bg-input-background shadow-sm">
                            <label className="block mb-3 font-semibold text-lg text-text-color">Pack Images</label>
                            <input
                                type="file"
                                multiple
                                accept="image/*"
                                onChange={handleImageFileChange}
                                className="block w-full text-sm text-gray-400 mb-4 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-violet-50 file:text-violet-700 hover:file:bg-violet-100 cursor-pointer"
                            />
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                                {currentPack.images?.filter(img => !img.isDeleted).map((img) => (
                                    <div key={img.id} className="relative group border border-gray-700 rounded-lg overflow-hidden shadow-sm aspect-w-16 aspect-h-9 flex items-center justify-center">
                                        <img src={img.url} alt={img.originalName} className="w-full h-full object-cover"/>
                                        <div className="absolute inset-0 bg-black bg-opacity-60 flex flex-col items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity p-2 text-center">
                                            <button type="button" onClick={() => handleRemoveMedia(img.id)} className="text-red-400 hover:text-red-300 absolute top-2 right-2 p-1 bg-gray-800 rounded-full"><XCircle size={20} /></button>
                                            <span className="text-white text-xs break-words leading-tight mt-6 px-1">{img.originalName}</span>
                                            <button type="button" onClick={() => handleSetPrimaryImage(img.id)} className={`mt-2 px-3 py-1 text-xs rounded-full font-semibold ${img.isPrimary ? 'bg-green-600 text-white' : 'bg-gray-300 text-gray-800 hover:bg-gray-200'}`}>
                                                {img.isPrimary ? 'Primary' : 'Set Primary'}
                                            </button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {LANGUAGES.map(lang => (
                            <div key={lang} className="p-3 border border-border rounded-md bg-input-background shadow-sm">
                                <h3 className="font-bold mb-2 text-text-color">{lang.toUpperCase()} Name / Description</h3>
                                <input name={`name_${lang}`} defaultValue={currentPack.names?.[lang] || ''} placeholder={`Pack Name (${lang})`} className="w-full bg-background p-2 rounded border border-gray-700 mb-2" required />
                                <textarea name={`desc_${lang}`} defaultValue={currentPack.descriptions?.[lang] || ''} placeholder={`Pack Description (${lang})`} className="w-full bg-background p-2 rounded mt-2 border border-gray-700" rows={3} />
                            </div>
                        ))}

                        <input name="price" type="number" step="0.01" defaultValue={currentPack.price || 0} placeholder="Price" className="w-full bg-background p-2 rounded border border-gray-700" required />

                        <div className="border border-border p-3 rounded-md bg-input-background shadow-sm">
                            <label className="block mb-2 font-semibold text-text-color">Products in this Pack</label>
                            <Select
                                isMulti
                                options={productsForSelect}
                                className="text-black"
                                classNamePrefix="select"
                                defaultValue={productsForSelect.filter(option => currentPack.productIds?.includes(option.value))}
                                onChange={(selectedOptions) => {
                                    const selectedIds = selectedOptions.map(option => option.value);
                                    // --- CORRECTION : Typage correct ---
                                    setCurrentPack(prev => {
                                        if (!prev) return prev;
                                        return {...prev, productIds: selectedIds};
                                    });
                                }}
                            />
                        </div>

                        <div className="flex justify-end pt-4">
                            <button type="submit" disabled={saving} className="bg-primary flex items-center px-6 py-2 rounded-lg text-primary-foreground font-semibold hover:bg-primary/90 disabled:bg-gray-600">
                                {saving && <Loader2 className="animate-spin mr-2"/>}
                                {saving ? 'Saving...' : 'Save Pack'}
                            </button>
                        </div>
                    </form>
                </Modal>
            )}
        </div>
    );
};

export default Packs;