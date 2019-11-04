package de.ellpeck.naturesaura.blocks;

import de.ellpeck.naturesaura.NaturesAura;
import de.ellpeck.naturesaura.blocks.tiles.TileEntityImpl;
import de.ellpeck.naturesaura.reg.IModItem;
import de.ellpeck.naturesaura.reg.IModelProvider;
import de.ellpeck.naturesaura.reg.ModRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;
import java.util.Random;

public class BlockContainerImpl extends ContainerBlock implements IModItem, IModelProvider {

    private final String baseName;

    private final Class<? extends TileEntity> tileClass;
    private final String tileRegName;

    public BlockContainerImpl(String baseName, Class<? extends TileEntity> tileClass, String tileReg, Block.Properties properties) {
        super(properties);

        this.baseName = baseName;
        this.tileClass = tileClass;
        this.tileRegName = tileReg;

        ModRegistry.add(this);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader world) {
        // TODO TYPES BLUTRGHGHGH
        try {
            return this.tileClass.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getBaseName() {
        return this.baseName;
    }

    public void onInit(FMLInitializationEvent event) {
        GameRegistry.registerTileEntity(this.tileClass, new ResourceLocation(NaturesAura.MOD_ID, this.tileRegName));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, BlockState state) {
        if (!worldIn.isRemote) {
            TileEntity tile = worldIn.getTileEntity(pos);
            if (tile instanceof TileEntityImpl)
                ((TileEntityImpl) tile).dropInventory();
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, BlockState state, int fortune) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityImpl)
            drops.add(((TileEntityImpl) tile).getDrop(state, fortune));
        else
            super.getDrops(drops, world, pos, state, fortune);
    }

    @Override
    public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest) {
        return willHarvest || super.removedByPlayer(state, world, pos, player, false);
    }

    @Override
    public void harvestBlock(World worldIn, PlayerEntity player, BlockPos pos, BlockState state, @Nullable TileEntity te, ItemStack stack) {
        super.harvestBlock(worldIn, player, pos, state, te, stack);
        worldIn.setBlockToAir(pos);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile instanceof TileEntityImpl)
            ((TileEntityImpl) tile).loadDataOnPlace(stack);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, BlockState state) {
        this.updateRedstoneState(worldIn, pos);
    }

    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        this.updateRedstoneState(worldIn, pos);
    }

    private void updateRedstoneState(World world, BlockPos pos) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityImpl) {
                TileEntityImpl impl = (TileEntityImpl) tile;
                int newPower = world.getRedstonePowerFromNeighbors(pos);
                if (impl.redstonePower != newPower)
                    world.scheduleUpdate(pos, this, this.tickRate(world));
            }
        }
    }

    @Override
    public int tickRate(World worldIn) {
        return 4;
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, BlockState state, Random rand) {
        if (!worldIn.isRemote) {
            TileEntity tile = worldIn.getTileEntity(pos);
            if (tile instanceof TileEntityImpl) {
                TileEntityImpl impl = (TileEntityImpl) tile;
                int newPower = worldIn.getRedstonePowerFromNeighbors(pos);
                if (impl.redstonePower != newPower)
                    impl.onRedstonePowerChange(newPower);
            }
        }
    }
}