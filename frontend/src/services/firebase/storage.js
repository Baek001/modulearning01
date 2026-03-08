import { getDownloadURL, ref, uploadBytes } from 'firebase/storage';
import { getFirebaseStorage } from './client';

function slugifySegment(value) {
    return String(value || '')
        .trim()
        .replace(/[^\w.-]+/g, '-')
        .replace(/-+/g, '-')
        .replace(/^-|-$/g, '')
        .toLowerCase();
}

export function buildStoragePath({ domain = 'misc', ownerId = 'anonymous', fileName = '', pathPrefix = '' }) {
    const safeDomain = slugifySegment(domain) || 'misc';
    const safeOwnerId = slugifySegment(ownerId) || 'anonymous';
    const safeFileName = slugifySegment(fileName.replace(/\.[^.]+$/, '')) || 'file';
    const extension = fileName.includes('.') ? fileName.slice(fileName.lastIndexOf('.')).toLowerCase() : '';
    const safePrefix = slugifySegment(pathPrefix);
    const timestamp = Date.now();

    return [safePrefix, safeDomain, safeOwnerId, `${timestamp}-${safeFileName}${extension}`]
        .filter(Boolean)
        .join('/');
}

export async function uploadStorageFile({ domain, ownerId, file, pathPrefix = '', metadata = {} }) {
    if (!file) {
        throw new Error('업로드할 파일이 없습니다.');
    }

    const fullPath = buildStoragePath({
        domain,
        ownerId,
        fileName: file.name,
        pathPrefix,
    });

    const storageRef = ref(getFirebaseStorage(), fullPath);
    const snapshot = await uploadBytes(storageRef, file, {
        contentType: file.type || 'application/octet-stream',
        customMetadata: Object.fromEntries(
            Object.entries(metadata || {}).map(([key, value]) => [key, String(value)])
        ),
    });
    const downloadURL = await getDownloadURL(snapshot.ref);

    return {
        name: file.name,
        size: file.size,
        contentType: file.type || 'application/octet-stream',
        path: snapshot.ref.fullPath,
        downloadURL,
    };
}

export async function getStorageDownloadUrl(path) {
    if (!path) {
        return '';
    }

    return getDownloadURL(ref(getFirebaseStorage(), path));
}
