package net.mehvahdjukaar.supplementaries.client.renderers.entities.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.mehvahdjukaar.supplementaries.common.entities.IQuiverEntity;
import net.mehvahdjukaar.supplementaries.common.items.QuiverItem;
import net.mehvahdjukaar.supplementaries.configs.ClientConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class QuiverLayer<T extends LivingEntity & IQuiverEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private final ItemRenderer itemRenderer;
    private final boolean isSkeleton;
    private final Supplier<QuiverMode> quiverMode;

    public QuiverLayer(RenderLayerParent<T, M> parent, boolean isSkeleton) {
        super(parent);
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
        this.isSkeleton = true;
        this.quiverMode = isSkeleton ? ClientConfigs.Items.QUIVER_SKELETON_RENDER_MODE : ClientConfigs.Items.QUIVER_RENDER_MODE;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T livingEntity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        QuiverMode mode = quiverMode.get();
        if (mode == QuiverMode.HIDDEN) return;

        ItemStack quiver = livingEntity.getQuiver();
        if (livingEntity.getMainHandItem() == quiver || livingEntity.getOffhandItem() == quiver) {
            return;
        }

        if (!quiver.isEmpty()) {
            this.getParentModel().body.translateAndRotate(poseStack);

            boolean flipped = livingEntity.getMainArm() == HumanoidArm.RIGHT;
            double o = 0.001; //to avoid z fight

            if (mode == QuiverMode.THIGH) {
                boolean hasArmor = livingEntity.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof ArmorItem;
                double offset = hasArmor ? ClientConfigs.Items.QUIVER_ARMOR_OFFSET.get() : 0;
                boolean sneaking = livingEntity.isCrouching();


                if(sneaking){
                    poseStack.translate(0,-0.125, -0.275);
                }
                float old;
                o += (offset == -1 ? 3.5 / 16f : 3 / 16f + offset);
                if (flipped) {
                    old = this.getParentModel().leftLeg.xRot;
                    this.getParentModel().leftLeg.xRot = old * 0.3f;
                    this.getParentModel().leftLeg.translateAndRotate(poseStack);
                    this.getParentModel().leftLeg.xRot = old;
                    poseStack.translate(0, -1 / 16f, -2.5 / 16f);
                    poseStack.translate(o, 0, 0);

                } else {
                    old = this.getParentModel().rightLeg.xRot;
                    this.getParentModel().rightLeg.xRot = old * 0.3f;
                    this.getParentModel().rightLeg.translateAndRotate(poseStack);
                    this.getParentModel().rightLeg.xRot = old;
                    poseStack.translate(0, -1 / 16f, -2.5 / 16f);
                    poseStack.translate(-o, 0, 0);
                }

            } else {
                boolean hasArmor = livingEntity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorItem;
                double offset = hasArmor ? ClientConfigs.Items.QUIVER_ARMOR_OFFSET.get() : 0;

                if (mode == QuiverMode.HIP) {
                    o += (offset == -1 ? 3.5 / 16f : 3 / 16f + offset);
                    poseStack.translate(0, 0.1, o);
                    poseStack.mulPose(Vector3f.YP.rotationDegrees(90));
                    if (flipped) poseStack.scale(-1, 1, -1);

                    poseStack.translate(0, 0.4, -3 / 16f);
                    poseStack.mulPose(Vector3f.XN.rotationDegrees(-22.5f));
                } else {
                    o += (offset == -1 ? 4 / 16f : 3 / 16f + offset);
                    poseStack.translate(0, 0.1, o);
                    poseStack.mulPose(Vector3f.YP.rotationDegrees(90));
                    if (flipped) poseStack.scale(-1, 1, -1);

                    poseStack.translate(0, 0, -0.125);
                }
            }
            itemRenderer.renderStatic(livingEntity, quiver, ItemTransforms.TransformType.HEAD, false,
                    poseStack, buffer, livingEntity.level, packedLight, OverlayTexture.NO_OVERLAY, 0);


        }
    }

    public enum QuiverMode {
        HIDDEN,
        BACK,
        HIP,
        THIGH
    }

}
