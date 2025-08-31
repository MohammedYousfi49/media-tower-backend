import { useEffect, useState, FormEvent } from 'react';
import axiosClient from '../../api/axiosClient';
import { PlusCircle, Trash2 } from 'lucide-react';

interface Tag {
    id: number;
    name: string;
}

const Tags = () => {
    const [tags, setTags] = useState<Tag[]>([]);
    const [newTagName, setNewTagName] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchTags();
    }, []);

    const fetchTags = async () => {
        setLoading(true);
        try {
            const response = await axiosClient.get<Tag[]>('/tags');
            setTags(response.data);
        } catch (error) {
            console.error('Failed to fetch tags:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleCreate = async (e: FormEvent) => {
        e.preventDefault();
        if (!newTagName.trim()) return;
        try {
            await axiosClient.post('/tags', { name: newTagName });
            setNewTagName('');
            fetchTags();
        } catch (error) {
            console.error('Failed to create tag:', error);
            alert('Failed to create tag. It might already exist.');
        }
    };

    const handleDelete = async (id: number) => {
        if (window.confirm('Are you sure you want to delete this tag? This might affect products associated with it.')) {
            try {
                await axiosClient.delete(`/tags/${id}`);
                fetchTags();
            } catch (error) {
                console.error('Failed to delete tag:', error);
            }
        }
    };

    return (
        <div>
            <h1 className="text-3xl font-bold text-white mb-6">Manage Tags</h1>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="bg-card p-4 rounded-lg border border-gray-700">
                    <h2 className="font-bold text-lg mb-4">Existing Tags</h2>
                    <div className="space-y-2">
                        {loading ? <p>Loading...</p> : tags.map(tag => (
                            <div key={tag.id} className="flex justify-between items-center bg-gray-800 p-2 rounded">
                                <span className="text-gray-300">{tag.name}</span>
                                <button onClick={() => handleDelete(tag.id)} className="text-red-500 hover:text-red-400">
                                    <Trash2 size={18}/>
                                </button>
                            </div>
                        ))}
                    </div>
                </div>
                <div className="bg-card p-4 rounded-lg border border-gray-700">
                    <h2 className="font-bold text-lg mb-4">Add New Tag</h2>
                    <form onSubmit={handleCreate} className="flex space-x-2">
                        <input
                            value={newTagName}
                            onChange={e => setNewTagName(e.target.value)}
                            placeholder="New tag name..."
                            className="flex-grow bg-gray-700 p-2 rounded border border-gray-600"
                        />
                        <button type="submit" className="bg-primary p-2 rounded text-white flex-shrink-0">
                            <PlusCircle/>
                        </button>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default Tags;