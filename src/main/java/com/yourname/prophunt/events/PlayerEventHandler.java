package com.yourname.prophunt.events;

import com.yourname.prophunt.PropHuntMod;
import com.yourname.prophunt.game.PropHuntGame;
import com.yourname.prophunt.teams.PropHuntTeam;
import com.yourname.prophunt.teams.TeamType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.UUID;
import java.util.Collection;

@EventBusSubscriber(modid = PropHuntMod.MODID)
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PropHuntGame game = PropHuntMod.getGameManager().getCurrentGame();

            if (game != null && game.isActive()) {
                PropHuntTeam team = game.getPlayerTeam(player.getUUID());

                if (team != null && team.getType() == TeamType.PROPS) {
                    // Convert prop to hunter when they die
                    game.convertPropToHunter(player.getUUID());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim && event.getSource().getEntity() instanceof ServerPlayer attacker) {
            PropHuntGame game = PropHuntMod.getGameManager().getCurrentGame();

            if (game != null && game.isActive()) {
                PropHuntTeam victimTeam = game.getPlayerTeam(victim.getUUID());
                PropHuntTeam attackerTeam = game.getPlayerTeam(attacker.getUUID());

                if (victimTeam != null && attackerTeam != null) {
                    // Props cannot damage props (friendly fire)
                    if (victimTeam.getType() == TeamType.PROPS && attackerTeam.getType() == TeamType.PROPS) {
                        event.setCanceled(true);
                    }

                    // Props cannot damage hunters (only knockback allowed)
                    else if (victimTeam.getType() == TeamType.HUNTERS && attackerTeam.getType() == TeamType.PROPS) {
                        // Cancel damage but keep knockback effect by setting damage to 0
                        event.setAmount(0.0f);
                    }
                }
            }
        }
    }
}
