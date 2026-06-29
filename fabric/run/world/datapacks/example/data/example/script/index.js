const counter = script.onProperty("example:counter", 0);

script.onCommand("foo/pos: position(freeform)", (source, { pos }) => {
    const world = source.world();
    world.particle({
        type: "heart",
        count: 100,
        offset: [0.5, 0.5, 0.5],
        speed: 0.05,
    }, pos)
    world.playSound({
        type: "minecraft:block.note_block.pling",
        category: "master",
        playback: {
            volume: 1,
            pitch: 1.5,
        }
    }, pos);
    const pig = world.spawn("minecraft:pig", pos);
    pig.inject("gravity", false);
})

script.onCommand("update/value: any()", (source, { value }) => {
    counter(value);
})