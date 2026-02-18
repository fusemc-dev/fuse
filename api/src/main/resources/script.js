
// Syringe API
entity.inject("name", "value");
entity.operate("name", (name) => [name, '!'])
entity.sample("foo");

// Round-trip serialization
entity.set("name", "value");
entity.get("foo");