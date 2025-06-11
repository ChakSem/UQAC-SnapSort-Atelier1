import { useState } from "react";

const SearchBar = ({ onSearch }: { onSearch: (prompt: string) => void }) => {

    const [value, setValue] = useState("");

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter") {
            onSearch(value);
        }
    };

    return (
        <div>
            <input
                type="text"
                value={value}
                onChange={e => setValue(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="🔍 Rechercher des images... Ajoutez des mots-clés ou décrivez la"
            />
        </div>
    );
}

export default SearchBar;