import { useEffect, useState, FormEvent } from 'react';
import { Save } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import { AxiosError } from 'axios';

interface Setting {
    key: string;
    value: string;
}

const Settings = () => {
    const [settings, setSettings] = useState<Setting[]>([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    useEffect(() => {
        fetchSettings();
    }, []);

    const fetchSettings = async () => {
        setLoading(true);
        try {
            const response = await axiosClient.get<Setting[]>('/settings');
            setSettings(response.data);
        } catch { // << CORRECTION 1: On enlève '_' qui n'est pas utilisé
            setError('Failed to load settings.');
        } finally {
            setLoading(false);
        }
    };

    const handleInputChange = (key: string, value: string) => {
        setSettings(currentSettings =>
            currentSettings.map(s => (s.key === key ? { ...s, value } : s))
        );
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setSaving(true);
        setError('');
        setSuccess('');
        try {
            await axiosClient.put('/settings', settings);
            setSuccess('Settings saved successfully!');
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            if (err instanceof AxiosError && err.response) {
                const responseData = err.response.data as { message?: string } | string;
                if (typeof responseData === 'string') {
                    setError(responseData);
                } else {
                    setError(responseData.message || 'Failed to save settings.');
                }
            } else {
                setError('An unexpected error occurred.');
            }
        } finally {
            setSaving(false);
        }
    };

    const renderSettingInput = (setting: Setting) => {
        const label = setting.key.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
        return (
            <div key={setting.key}>
                <label htmlFor={setting.key} className="block text-sm font-medium text-gray-300 mb-1">{label}</label>
                <input
                    type="text"
                    id={setting.key}
                    value={setting.value}
                    onChange={(e) => handleInputChange(setting.key, e.target.value)}
                    className="w-full bg-gray-700 text-white p-2 rounded-md border border-gray-600 focus:outline-none focus:ring-2 focus:ring-primary"
                />
            </div>
        );
    };

    if (loading) return <div className="text-center p-8">Loading settings...</div>;

    return (
        <div>
            <h1 className="text-3xl font-bold text-white mb-6">Site Settings</h1>
            <form onSubmit={handleSubmit} className="bg-card p-6 rounded-lg border border-gray-700 space-y-8">
                <div>
                    <h2 className="text-xl font-semibold text-white border-b border-gray-600 pb-2 mb-4">General</h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {settings.filter(s => !s.key.startsWith('social_')).map(renderSettingInput)}
                    </div>
                </div>

                <div>
                    <h2 className="text-xl font-semibold text-white border-b border-gray-600 pb-2 mb-4">Social Media Links</h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {/* << CORRECTION 2: J'ai corrigé la faute de frappe ici */}
                        {settings.filter(s => s.key.startsWith('social_')).map(renderSettingInput)}
                    </div>
                </div>

                <div className="pt-4 border-t border-gray-700">
                    {error && <p className="text-red-400 mb-4 text-center">{error}</p>}
                    {success && <p className="text-green-400 mb-4 text-center">{success}</p>}

                    <div className="flex justify-end">
                        <button
                            type="submit"
                            disabled={saving}
                            className="bg-primary text-white px-6 py-2 rounded-lg flex items-center hover:bg-blue-600 transition-colors disabled:bg-gray-500 disabled:cursor-not-allowed"
                        >
                            <Save className="mr-2" size={20} />
                            {saving ? 'Saving...' : 'Save Settings'}
                        </button>
                    </div>
                </div>
            </form>
        </div>
    );
};

export default Settings;