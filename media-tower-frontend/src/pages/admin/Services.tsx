// Fichier : src/pages/admin/Services.tsx (NETTOYÉ ET FINAL)

import { useEffect, useState, FormEvent, ChangeEvent } from 'react';
// --- CORRECTION : Suppression des imports inutilisés ---
import { PlusCircle, Edit, Trash2, Loader2, XCircle } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Modal from '../../components/shared/Modal';
// 'AxiosError' a été supprimé car non utilisé

// --- Interfaces ---
interface Media {
    id: number;
    url: string;
    originalName: string;
    isPrimary: boolean;
    file?: File;
    isNew?: boolean;
    isDeleted?: boolean;
}

interface Service {
    id: number;
    names: { [key: string]: string };
    descriptions: { [key: string]: string };
    price: number;
    images: Media[];
}

const LANGUAGES: Array<'fr' | 'en'> = ['fr', 'en'];

const Services = () => {
    const [services, setServices] = useState<Service[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [currentService, setCurrentService] = useState<Partial<Service>>({});
    const [saving, setSaving] = useState(false);
    const [loading, setLoading] = useState(true);

    const fetchData = async () => {
        setLoading(true);
        try {
            const response = await axiosClient.get<Service[]>('/services');
            setServices(response.data);
        } catch (error) {
            console.error('Failed to fetch services:', error);
            alert('Failed to load data.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void fetchData();
    }, []);

    const handleOpenModal = (service: Partial<Service> | null = null) => {
        const initialService: Partial<Service> = {
            names: { fr: '', en: '' },
            descriptions: { fr: '', en: '' },
            price: 0,
            images: [],
        };
        setCurrentService({
            ...initialService,
            ...service,
            names: { ...initialService.names, ...service?.names },
            descriptions: { ...initialService.descriptions, ...service?.descriptions },
            images: service?.images ? [...service.images] : [],
        });
        setIsModalOpen(true);
    };

    const handleCloseModal = () => setIsModalOpen(false);

    const handleSave = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setSaving(true);
        const form = e.currentTarget;

        const serviceDto = {
            names: { fr: (form.name_fr as HTMLInputElement).value, en: (form.name_en as HTMLInputElement).value },
            descriptions: { fr: (form.desc_fr as HTMLTextAreaElement).value, en: (form.desc_en as HTMLTextAreaElement).value },
            price: Number((form.price as HTMLInputElement).value),
        };

        const formData = new FormData();
        formData.append("serviceDto", new Blob([JSON.stringify(serviceDto)], { type: "application/json" }));

        currentService.images?.filter(img => img.isNew && img.file).forEach(media => {
            if (media.file) formData.append("newImages", media.file);
        });

        try {
            if (currentService?.id) {
                await axiosClient.put(`/services/${currentService.id}`, formData);
            } else {
                await axiosClient.post('/services', formData);
            }
            await fetchData();
            handleCloseModal();
        } catch (error) {
            console.error('Failed to save service', error);
            alert('Error saving service. Check console for details.');
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (id: number) => {
        if (window.confirm('Are you sure you want to delete this service?')) {
            try {
                await axiosClient.delete(`/services/${id}`);
                setServices(prev => prev.filter(s => s.id !== id));
                alert('Service deleted successfully!');
            } catch (error) {
                console.error('Failed to delete service', error);
                alert('Error deleting service.');
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

            setCurrentService(prev => {
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
        setCurrentService(prev => {
            if (!prev) return prev;
            return {
                ...prev,
                images: prev.images?.map(img => ({ ...img, isPrimary: img.id === id })) || []
            }
        });
    };

    const handleRemoveMedia = (id: number) => {
        setCurrentService(prev => {
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
                <h1 className="text-3xl font-bold text-white">Manage Services</h1>
                <button
                    onClick={() => handleOpenModal()}
                    className="bg-primary text-primary-foreground px-4 py-2 rounded-lg flex items-center hover:bg-primary/90 transition-colors shadow-md"
                >
                    <PlusCircle className="mr-2" size={20} /> Add New Service
                </button>
            </div>

            <div className="bg-card p-4 rounded-lg border border-border overflow-x-auto shadow-lg">
                <table className="w-full text-left table-auto">
                    <thead>
                    <tr className="border-b border-border text-sm text-gray-400 uppercase">
                        <th className="p-3 w-16">Image</th>
                        <th className="p-3">Name (EN)</th>
                        <th className="p-3 w-28 text-right">Price (DH)</th>
                        <th className="p-3 w-24 text-center">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {loading ? (
                        <tr><td colSpan={4} className="text-center p-8"><Loader2 className="animate-spin inline-block mr-2 text-primary" size={24} /> Loading services...</td></tr>
                    ) : services.map(service => {
                        const primaryImage = service.images?.find(img => img.isPrimary) || service.images?.[0];
                        return (
                            <tr key={service.id} className="border-b border-border last:border-b-0 hover:bg-hover-card transition-colors">
                                <td className="p-2">
                                    <img src={primaryImage?.url || 'https://via.placeholder.com/40x40?text=No+Img'} alt={service.names.en} className="w-12 h-12 rounded object-cover border border-gray-700" />
                                </td>
                                <td className="p-3">{service.names.en}</td>
                                <td className="p-3 text-right">{service.price.toFixed(2)}</td>
                                <td className="flex space-x-2 p-3 justify-center items-center h-full">
                                    <button onClick={() => handleOpenModal(service)} className="text-yellow-400 hover:text-yellow-300"><Edit size={18} /></button>
                                    <button onClick={() => handleDelete(service.id)} className="text-red-500 hover:text-red-400"><Trash2 size={18} /></button>
                                </td>
                            </tr>
                        );
                    })}
                    </tbody>
                </table>
            </div>

            {isModalOpen && currentService && (
                <Modal isOpen={isModalOpen} onClose={handleCloseModal} title={currentService.id ? 'Edit Service' : 'Add New Service'}>
                    <form onSubmit={handleSave} className="space-y-6 max-h-[80vh] overflow-y-auto p-2 scrollbar-thin scrollbar-thumb-gray-600 scrollbar-track-gray-800">

                        <div className="border border-border p-4 rounded-md bg-input-background shadow-sm">
                            <label className="block mb-3 font-semibold text-lg text-text-color">Service Images</label>
                            <input
                                type="file"
                                multiple
                                accept="image/*"
                                onChange={handleImageFileChange}
                                className="block w-full text-sm text-gray-400 mb-4 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-violet-50 file:text-violet-700 hover:file:bg-violet-100 cursor-pointer"
                            />
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                                {currentService.images?.filter(img => !img.isDeleted).map((img) => (
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
                                <input name={`name_${lang}`} defaultValue={currentService.names?.[lang] || ''} placeholder={`Service Name (${lang})`} className="w-full bg-background p-2 rounded border border-gray-700 mb-2" required />
                                <textarea name={`desc_${lang}`} defaultValue={currentService.descriptions?.[lang] || ''} placeholder={`Service Description (${lang})`} className="w-full bg-background p-2 rounded mt-2 border border-gray-700" rows={3} />
                            </div>
                        ))}

                        <input name="price" type="number" step="0.01" defaultValue={currentService.price || 0} placeholder="Price" className="w-full bg-background p-2 rounded border border-gray-700" required />

                        <div className="flex justify-end pt-4">
                            <button type="submit" disabled={saving} className="bg-primary flex items-center px-6 py-2 rounded-lg text-primary-foreground font-semibold hover:bg-primary/90 disabled:bg-gray-600">
                                {saving && <Loader2 className="animate-spin mr-2"/>}
                                {saving ? 'Saving...' : 'Save Service'}
                            </button>
                        </div>
                    </form>
                </Modal>
            )}
        </div>
    );
};

export default Services;