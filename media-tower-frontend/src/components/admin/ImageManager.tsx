// Fichier : src/components/admin/ImageManager.tsx (Corrigé)

import { ChangeEvent, Dispatch, SetStateAction } from 'react';
import { X, CheckCircle } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

interface Media {
    id: number;
    url: string;
    isPrimary: boolean;
}

interface ImageManagerProps {
    entityType: 'product' | 'service' | 'pack';
    entityId: number | undefined; // L'ID peut être indéfini lors de la création
    existingImages: Media[];
    selectedFiles: File[];
    setSelectedFiles: Dispatch<SetStateAction<File[]>>;
    onDataChange: () => void;
}

const ImageManager = ({ entityType, entityId, existingImages, selectedFiles, setSelectedFiles, onDataChange }: ImageManagerProps) => {

    const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files) {
            // --- DÉBUT DE LA CORRECTION ---
            // On convertit FileList en Array<File> explicitement
            const newFiles = Array.from(e.target.files);
            setSelectedFiles(prev => [...prev, ...newFiles]);
            // --- FIN DE LA CORRECTION ---
        }
    };

    const removeSelectedFile = (fileName: string) => {
        setSelectedFiles(prev => prev.filter(file => file.name !== fileName));
    };

    const handleDeleteExisting = async (mediaId: number) => {
        if (window.confirm('Are you sure you want to delete this image permanently?')) {
            try {
                await axiosClient.delete(`/media/${mediaId}`);
                onDataChange();
            } catch (error) {
                console.error('Failed to delete image:', error);
                alert('Could not delete the image.');
            }
        }
    };

    const handleSetPrimary = async (mediaId: number) => {
        // On ne peut pas définir une image principale pour une entité qui n'est pas encore créée (pas d'ID)
        if (!entityId) {
            alert("Please save the item first before setting a primary image.");
            return;
        }
        try {
            await axiosClient.post(`/media/set-primary/${entityType}/${entityId}/media/${mediaId}`);
            onDataChange();
        } catch (error) {
            console.error('Failed to set primary image:', error);
            alert('Could not set primary image.');
        }
    };

    return (
        <div className="p-4 border border-gray-600 rounded-md space-y-4">
            <h3 className="text-lg font-bold text-white">Manage Images</h3>
            <div>
                <label className="block text-sm text-gray-400 mb-1">Add new images</label>
                <input
                    type="file"
                    multiple
                    onChange={handleFileChange}
                    // On vide le champ pour permettre de sélectionner le même fichier à nouveau après l'avoir supprimé
                    onClick={(e) => (e.currentTarget.value = '')}
                    className="w-full bg-gray-700 text-white p-2 rounded-md border border-gray-600 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-primary file:text-white hover:file:bg-opacity-80"
                />
            </div>

            <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-4">
                {/* Images existantes */}
                {existingImages.map(image => (
                    <div key={image.id} className="relative group aspect-square">
                        <img src={image.url} alt="Existing" className="w-full h-full object-cover rounded-md" />
                        <div className="absolute inset-0 bg-black bg-opacity-50 flex flex-col items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity rounded-md">
                            {!image.isPrimary && (
                                <button type="button" onClick={() => handleSetPrimary(image.id)} className="text-white bg-green-600 hover:bg-green-700 rounded-full p-1.5 mb-2" title="Set as primary">
                                    <CheckCircle size={20} />
                                </button>
                            )}
                            <button type="button" onClick={() => handleDeleteExisting(image.id)} className="text-white bg-red-600 hover:bg-red-700 rounded-full p-1.5" title="Delete image">
                                <X size={20} />
                            </button>
                        </div>
                        {image.isPrimary && (
                            <div className="absolute top-1 left-1 bg-green-600 text-white text-xs font-bold px-2 py-0.5 rounded-full">Primary</div>
                        )}
                    </div>
                ))}
                {/* Nouveaux fichiers sélectionnés */}
                {selectedFiles.map((file, index) => (
                    <div key={index} className="relative group aspect-square">
                        <img src={URL.createObjectURL(file)} alt="New file" className="w-full h-full object-cover rounded-md" />
                        <div className="absolute top-1 right-1">
                            <button type="button" onClick={() => removeSelectedFile(file.name)} className="text-white bg-red-600 hover:bg-red-700 rounded-full p-1.5" title="Remove file">
                                <X size={20} />
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default ImageManager;