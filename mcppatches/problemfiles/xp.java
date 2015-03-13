package net.minecraft.src;

import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

public class xp implements Predicate
{
    public final EntityLivingBase a;

    public xp(EntityLivingBase p_i46446_1_)
    {
        this.a = p_i46446_1_;
    }

    public boolean a(Entity p_a_1_)
    {
        return p_a_1_.canBePushed();
    }

    public boolean apply(Object p_apply_1_)
    {
        return this.a((Entity)p_apply_1_);
    }
}
