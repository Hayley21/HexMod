package at.petrak.hexcasting.common.casting.operators.spells

import at.petrak.hexcasting.api.Operator.Companion.getChecked
import at.petrak.hexcasting.api.RenderedSpell
import at.petrak.hexcasting.api.SpellDatum
import at.petrak.hexcasting.api.SpellOperator
import at.petrak.hexcasting.common.casting.CastingContext
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3


object OpTheOnlyReasonAnyoneDownloadedPsi : SpellOperator {
    override val argc = 1
    override fun execute(args: List<SpellDatum<*>>, ctx: CastingContext): Triple<RenderedSpell, Int, List<Vec3>> {
        val target = args.getChecked<Vec3>(0)
        ctx.assertVecInRange(target)

        return Triple(
            Spell(target),
            10_000,
            listOf(target)
        )
    }

    private data class Spell(val target: Vec3) : RenderedSpell {
        override fun cast(ctx: CastingContext) {
            // https://github.com/VazkiiMods/Psi/blob/master/src/main/java/vazkii/psi/common/spell/trick/PieceTrickOvergrow.java
            val pos = BlockPos(target)
            val hit = BlockHitResult(Vec3.ZERO, Direction.UP, pos, false)
            val save: ItemStack = ctx.caster.getItemInHand(InteractionHand.MAIN_HAND)
            ctx.caster.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.BONE_MEAL))
            val fakeContext = UseOnContext(ctx.caster, InteractionHand.MAIN_HAND, hit)
            ctx.caster.setItemInHand(InteractionHand.MAIN_HAND, save)
            Items.BONE_MEAL.useOn(fakeContext)
        }
    }
}