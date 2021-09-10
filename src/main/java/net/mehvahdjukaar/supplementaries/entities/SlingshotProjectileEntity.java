package net.mehvahdjukaar.supplementaries.entities;

import net.mehvahdjukaar.supplementaries.common.CommonUtil;
import net.mehvahdjukaar.supplementaries.configs.ServerConfigs;
import net.mehvahdjukaar.supplementaries.events.ItemsOverrideHandler;
import net.mehvahdjukaar.supplementaries.setup.ModRegistry;
import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.FMLPlayMessages;
import net.minecraftforge.fml.network.NetworkHooks;

public class SlingshotProjectileEntity extends ImprovedProjectileEntity implements IEntityAdditionalSpawnData {
    private static final DataParameter<Byte> ID_LOYALTY = EntityDataManager.defineId(SlingshotProjectileEntity.class, DataSerializers.BYTE);
    //only client
    public int clientSideReturnTridentTickCount;
    private final float xRotInc;
    private final float yRotInc;
    private float particleCooldown = 0;

    public SlingshotProjectileEntity(LivingEntity thrower, World world, ItemStack item, ItemStack throwerStack) {
        super(ModRegistry.SLINGSHOT_PROJECTILE.get(), thrower, world);
        this.setItem(item);
        this.entityData.set(ID_LOYALTY, (byte) EnchantmentHelper.getLoyalty(throwerStack));
        this.maxAge = 500;
        this.xRotInc = 0;
        this.yRotInc = 0;
    }

    public SlingshotProjectileEntity(FMLPlayMessages.SpawnEntity spawnEntity, World world) {
        this(ModRegistry.SLINGSHOT_PROJECTILE.get(), world);
    }

    //client factory
    public SlingshotProjectileEntity(EntityType<SlingshotProjectileEntity> type, World world) {
        super(type, world);
        this.maxAge = 500;
        this.yRotInc = (this.random.nextBoolean() ? 1 : -1) * (float) (4 * this.random.nextGaussian() + 7);
        this.xRotInc = (this.random.nextBoolean() ? 1 : -1) * (float) (4 * this.random.nextGaussian() + 7);
        this.xRot = this.random.nextFloat() * 360;
        this.yRot = this.random.nextFloat() * 360;
        this.xRotO = this.xRot;
        this.yRotO = this.yRot;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ID_LOYALTY, (byte) 0);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(ID_LOYALTY, tag.getByte("Loyalty"));
    }

    public void addAdditionalSaveData(CompoundNBT tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Loyalty", this.entityData.get(ID_LOYALTY));
    }

    public void setLoyalty(ItemStack stack) {
        this.entityData.set(ID_LOYALTY, (byte) EnchantmentHelper.getLoyalty(stack));
    }

    @Override
    protected Item getDefaultItem() {
        return Items.STONE;
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult entityRayTraceResult) {
        super.onHitEntity(entityRayTraceResult);
        Entity entity = entityRayTraceResult.getEntity();
        if (entity instanceof EndermanEntity) {
            EndermanEntity enderman = ((EndermanEntity) entity);
            Item item = this.getItem().getItem();
            if (item instanceof BlockItem) {
                Block block = ((BlockItem) item).getBlock();
                if (block.is(BlockTags.ENDERMAN_HOLDABLE) || ServerConfigs.cached.UNRESTRICTED_SLINGSHOT) {
                    if (enderman.getCarriedBlock() == null) {
                        enderman.setCarriedBlock(block.defaultBlockState());
                        this.remove();
                    }
                }
            }
        }
    }

    @Override
    protected void onHitBlock(BlockRayTraceResult hit) {
        super.onHitBlock(hit);
        //can only place when first hits
        if (this.touchedGround) return;
        Entity owner = this.getOwner();
        boolean success = false;
        if (owner == null || owner instanceof PlayerEntity && ((PlayerEntity) owner).abilities.mayBuild) {

            if (owner == null && level.isClientSide) {

                //idk why but sound isn't played with null entity. also owner is not synced and idk how to get it from client
                //TODO: change this. this it will cause issues
                owner = CommonUtil.getClientPlayer();
                //this thing really doesn't like null players
                //why is it not playing a placement sound when called on server side :waaaa:
            }

            if (owner != null) {
                ItemStack stack = this.getItem();
                Item item = stack.getItem();
                //block override. mimic forge event
                PlayerInteractEvent.RightClickBlock blockPlaceEvent = new PlayerInteractEvent.RightClickBlock((PlayerEntity) owner, Hand.MAIN_HAND, hit.getBlockPos(), hit);
                ItemsOverrideHandler.tryPerformOverride(blockPlaceEvent, stack, true);

                if (blockPlaceEvent.isCanceled() && blockPlaceEvent.getCancellationResult().consumesAction()) {
                    success = true;
                }
                if (!success && item instanceof BlockItem) {
                    BlockItemUseContext ctx = new BlockItemUseContext(this.level, (PlayerEntity) owner, Hand.MAIN_HAND, this.getItem(), hit);
                    success = ((BlockItem) item).place(ctx).consumesAction();
                }
                if (success) this.remove();
            }
        }
    }

    @Override
    public boolean hasReachedEndOfLife() {
        return super.hasReachedEndOfLife() && !this.isNoPhysics();
    }

    @Override
    public void tick() {

        if (this.isNoPhysics()) {
            int i = this.entityData.get(ID_LOYALTY);
            Entity owner = this.getOwner();
            if (i > 0 && this.isAcceptableReturnOwner(owner)) {
                Vector3d vector3d = new Vector3d(owner.getX() - this.getX(), owner.getEyeY() - this.getY(), owner.getZ() - this.getZ());
                this.setPosRaw(this.getX(), this.getY() + vector3d.y * 0.015D * (double) i, this.getZ());
                if (this.level.isClientSide) {
                    this.yOld = this.getY();
                }

                double d0 = 0.05D * (double) i;
                this.setDeltaMovement(this.getDeltaMovement().scale(0.95D).add(vector3d.normalize().scale(d0)));

                ++this.clientSideReturnTridentTickCount;
            }
        }
        super.tick();
    }

    private boolean isAcceptableReturnOwner(Entity owner) {
        if (owner != null && owner.isAlive()) {
            return !(owner instanceof ServerPlayerEntity) || !owner.isSpectator();
        } else {
            return false;
        }
    }

    @Override
    public void playerTouch(PlayerEntity playerEntity) {
        if (this.isNoPhysics() || this.touchedGround) {

            boolean success = playerEntity.abilities.instabuild || playerEntity.inventory.add(this.getItem());

            if (!this.level.isClientSide) {
                if (!success) {
                    this.spawnAtLocation(this.getItem(), 0.1f);
                }
            } else {
                this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, (this.random.nextFloat() - this.random.nextFloat()) * 1.4F + 2.0F, false);
            }
            this.remove();
        }
    }

    @Override
    public void reachedEndOfLife() {
        if (this.entityData.get(ID_LOYALTY) != 0 && this.isAcceptableReturnOwner(this.getOwner())) {
            this.setNoPhysics(true);
            this.groundTime = 0;
        } else {
            this.spawnAtLocation(this.getItem(), 0.1f);
            super.reachedEndOfLife();
        }
    }


    @Override
    protected void updateRotation() {

        if (!this.isNoPhysics()) {
            this.xRotO = this.xRot;
            this.yRotO = this.yRot;
            this.xRot += xRotInc;
            this.yRot += yRotInc;
            this.particleCooldown++;
        } else {
            super.updateRotation();
        }

    }

    @Override
    public void spawnTrailParticles(Vector3d currentPos, Vector3d newPos) {
        if (!this.isNoPhysics()) {
            double d = this.getDeltaMovement().length();
            if (this.tickCount > 2 && d * this.tickCount > 1.8) {
                double p = 4 / (d * 0.95 + 0.05);
                if (this.particleCooldown > p) {
                    this.particleCooldown -= p;
                    double x = currentPos.x;
                    double y = currentPos.y + this.getBbHeight() / 2d;
                    double z = currentPos.z;
                    this.level.addParticle(ModRegistry.SLINGSHOT_PARTICLE.get(), x, y, z, 0, 0.02, 0);
                }
            }
        }
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        Entity entity = this.getOwner();
        int id = -1;
        if (entity != null) {
            id = entity.getId();
        }
        buffer.writeInt(id);
    }

    @Override
    public void readSpawnData(PacketBuffer buffer) {
        int id = buffer.readInt();
        if (id != -1) {
            this.setOwner(this.level.getEntity(id));
        }
    }
}