// Types pour le média
export type MediaFile = {
    path: string;
    name: string;
    isVideo: boolean;
    thumbnailPath: string;
}

export type Folder = {
    name: string;
    path: string;
    children: Folder[];
}

export type Status = 'no-loading' | 'loading' | 'extended-loading';