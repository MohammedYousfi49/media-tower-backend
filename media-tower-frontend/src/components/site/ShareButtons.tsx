import { Facebook, Twitter, Linkedin, Copy } from 'lucide-react';

interface ShareButtonsProps {
    url: string; // L'URL de la page à partager
    title: string; // Le titre qui sera utilisé pour le partage
}

const ShareButtons = ({ url, title }: ShareButtonsProps) => {

    const encodedUrl = encodeURIComponent(url);
    const encodedTitle = encodeURIComponent(title);

    const copyToClipboard = () => {
        navigator.clipboard.writeText(url).then(() => {
            alert('Link copied to clipboard!');
        }, (err) => {
            console.error('Could not copy text: ', err);
        });
    };

    return (
        <div className="flex items-center space-x-4">
            <span className="font-semibold text-gray-400">Share:</span>
            <a
                href={`https://www.facebook.com/sharer/sharer.php?u=${encodedUrl}`}
                target="_blank"
                rel="noopener noreferrer"
                className="text-gray-400 hover:text-blue-600 transition-colors"
                title="Share on Facebook"
            >
                <Facebook size={20} />
            </a>
            <a
                href={`https://twitter.com/intent/tweet?url=${encodedUrl}&text=${encodedTitle}`}
                target="_blank"
                rel="noopener noreferrer"
                className="text-gray-400 hover:text-blue-400 transition-colors"
                title="Share on Twitter"
            >
                <Twitter size={20} />
            </a>
            <a
                href={`https://www.linkedin.com/shareArticle?mini=true&url=${encodedUrl}&title=${encodedTitle}`}
                target="_blank"
                rel="noopener noreferrer"
                className="text-gray-400 hover:text-blue-700 transition-colors"
                title="Share on LinkedIn"
            >
                <Linkedin size={20} />
            </a>
            <button
                onClick={copyToClipboard}
                className="text-gray-400 hover:text-white transition-colors"
                title="Copy link"
            >
                <Copy size={20} />
            </button>
        </div>
    );
};

export default ShareButtons;