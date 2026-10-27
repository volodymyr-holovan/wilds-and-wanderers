// Wilds & Wanderers: player-facing commands behind the inventory sidebar buttons.
// Pufferfish's Skills only exposes "open" to operators, so the server opens the tree on the player's behalf.

ServerEvents.commandRegistry(event => {
  const { commands: Commands } = event

  event.register(
    Commands.literal('wilds')
      .then(Commands.literal('skills').executes(ctx => {
        const player = ctx.source.playerOrException
        ctx.source.server.runCommandSilent(`puffish_skills open ${player.username}`)
        return 1
      }))
  )
})
