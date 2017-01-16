package io.github.cccm5;

import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
/**
* CragoTrait is a marker trait that needs to be added to all dtl traders that can interact with Movecraft crafts
*/
@TraitName("cargo")
public class CargoTrait extends Trait
{

    public CargoTrait()
    {
        super("cargo");
    }

    @Override
    public void onAttach(){
        if(!this.npc.hasTrait(net.dandielo.citizens.traders_v3.traits.TraderTrait.class))
            this.npc.removeTrait(CargoTrait.class);
    }

}
