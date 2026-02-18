script.on("join", (player) => {
    player.sendMessage(["Hello, ", player.sample("minecraft:name"), "!"])
    player.sendMessage(`${player.sample("minecraft:position")}`)
})