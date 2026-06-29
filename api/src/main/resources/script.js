
script.on("foo:bar[guard(value) | another(value)]", () => {

})

script.onCommand("foo/bar/a: integer/b: integer", (ctx) => {
    const { a, b } = ctx.args();
    return a + b;
})