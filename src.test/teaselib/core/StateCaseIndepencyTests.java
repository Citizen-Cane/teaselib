package teaselib.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import teaselib.Body;
import teaselib.HouseHold;
import teaselib.TeaseScript;
import teaselib.Toys;
import teaselib.test.TestScript;

public class StateCaseIndepencyTests {

    @Test
    public void testCaseIndepencencyOfState() {
        TeaseScript script = TestScript.getOne();

        assertFalse(script.state(Toys.Collar).applied());
        assertTrue(script.state(Toys.Collar).expired());

        script.state(Toys.Collar).apply();

        assertTrue(script.state(Toys.Collar).is(script.namespace));
        assertTrue(script.state("teaselib.Toys.Collar").is(script.namespace));
        assertTrue(script.state("TeaseLib.toys.collar").is(script.namespace));
        assertTrue(script.state("teaselib.toys.COLLAR").is(script.namespace));
        assertTrue(script.state("TEASELIB.Toys.cOLLAR").applied());

    }

    @Test
    public void testCaseIndepencencyOfStatePeersAndAttributes() {
        TeaseScript script = TestScript.getOne();

        script.state("teaselib.household.clothes_pegs").apply("teaseLib.body.somethingonnipples");
        assertTrue(script.state(Body.SomethingOnNipples).applied());
        assertTrue(script.state("TeaseLib.Body.SomethingOnNipples").applied());

        assertTrue(script.state(HouseHold.Clothes_Pegs).applied());
        assertTrue(script.state(HouseHold.Clothes_Pegs).is(script.namespace));
        assertTrue(script.state("teaselib.household.clothes_pegs").is(script.namespace));

        assertTrue(script.state("teaselib.Household.Clothes_Pegs").is(script.namespace));
        assertTrue(script.state("teaselib.Household.Clothes_Pegs").is(script.namespace.toLowerCase()));

        assertTrue(script.state(Body.SomethingOnNipples).is(HouseHold.Clothes_Pegs));
        assertTrue(script.state(Body.SomethingOnNipples).is("teaselib.Household.Clothes_Pegs"));
        assertTrue(script.state(Body.SomethingOnNipples).is("TEASELIB.household.cLOTHES_pEGS"));

        assertTrue(script.state(HouseHold.Clothes_Pegs).is("teaseLib.Body.SomethingOnNipples"));
        assertTrue(script.state(HouseHold.Clothes_Pegs).is("teaseLib.body.somethingonnipples"));
    }

    @Test
    public void testCaseIndepencencyOfItems() {
        TeaseScript script = TestScript.getOne();

        assertFalse(script.item(Toys.Collar).applied());
        assertTrue(script.item(Toys.Collar).expired());

        script.item(Toys.Collar).apply();

        assertTrue(script.item(Toys.Collar).is(script.namespace));
        assertTrue(script.item("teaselib.Toys.Collar").is(script.namespace));
        assertTrue(script.item("TeaseLib.toys.collar").is(script.namespace));
        assertTrue(script.item("teaselib.toys.COLLAR").is(script.namespace));
        assertTrue(script.item("TEASELIB.Toys.cOLLAR").applied());

    }

    @Test
    public void testCaseIndepencencyOfItemAttributes() {
        TeaseScript script = TestScript.getOne();

        script.item("teaselib.household.clothes_pegs").to("teaseLib.body.somethingonnipples");
        assertTrue(script.item(Body.SomethingOnNipples).applied());
        assertTrue(script.item("TeaseLib.Body.SomethingOnNipples").applied());

        assertTrue(script.item(HouseHold.Clothes_Pegs).applied());
        assertTrue(script.item(HouseHold.Clothes_Pegs).is(script.namespace));
        assertTrue(script.item("teaselib.household.clothes_pegs").is(script.namespace));

        assertTrue(script.item("teaselib.Household.Clothes_Pegs").is(script.namespace));
        assertTrue(script.item("teaselib.Household.Clothes_Pegs").is(script.namespace.toLowerCase()));

        assertTrue(script.item(Body.SomethingOnNipples).is(HouseHold.Clothes_Pegs));
        assertTrue(script.item(Body.SomethingOnNipples).is("teaselib.Household.Clothes_Pegs"));
        assertTrue(script.item(Body.SomethingOnNipples).is("TEASELIB.household.cLOTHES_pEGS"));

        assertTrue(script.item(HouseHold.Clothes_Pegs).is("teaseLib.Body.SomethingOnNipples"));
        assertTrue(script.item(HouseHold.Clothes_Pegs).is("teaseLib.body.somethingonnipples"));
    }
}
