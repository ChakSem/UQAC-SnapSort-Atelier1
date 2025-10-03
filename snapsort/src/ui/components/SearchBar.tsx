import { useState, forwardRef, useImperativeHandle } from "react";

export interface SearchBarRef {
    getValue: () => string;
}

const SearchBar = forwardRef<SearchBarRef, { onSearch: (prompt: string) => void }>(({ onSearch }, ref) => {
    const [value, setValue] = useState("");

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter") {
            onSearch(value);
        }
    };

    useImperativeHandle(ref, () => ({
        getValue: () => value
    }));

    return (
        <div className="search-bar">
            <input
                type="text"
                value={value}
                onChange={e => setValue(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="🔍 Rechercher des images... Ajoutez des mots-clés ou décrivez la"
            />
        </div>
    );
});

export default SearchBar;