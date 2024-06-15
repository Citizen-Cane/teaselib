package teaselib.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.Body;
import teaselib.Household;
import teaselib.Toys;
import teaselib.test.TestScript;

import java.io.IOException;

public class StateCaseIndepencyTests {

    @Test
    public void testCaseIndepencencyOfState() throws IOException {
        try (TestScript script = new TestScript()) {
            Assertions.assertFalse(script.state(Toys.Collar).applied());
            Assertions.assertTrue(script.state(Toys.Collar).expired());

            script.state(Toys.Collar).apply();

            Assertions.assertTrue(script.state(Toys.Collar).is(script.namespace));
            Assertions.assertTrue(script.state("teaselib.Toys.Collar").is(script.namespace));
            Assertions.assertTrue(script.state("TeaseLib.toys.collar").is(script.namespace));
            Assertions.assertTrue(script.state("teaselib.toys.COLLAR").is(script.namespace));
            Assertions.assertTrue(script.state("TEASELIB.Toys.cOLLAR").applied());
        }

    }

    @Test
    public void testCaseIndepencencyOfStatePeersAndAttributes() throws IOException {
        try (TestScript script = new TestScript()) {
            script.state("teaselib.household.clothes_pegs").applyTo("teaseLib.body.onnipples");
            Assertions.assertTrue(script.state(Body.OnNipples).applied());
            Assertions.assertTrue(script.state("TeaseLib.Body.OnNipples").applied());

            Assertions.assertTrue(script.state(Household.Clothes_Pegs).applied());
            Assertions.assertTrue(script.state(Household.Clothes_Pegs).is(script.namespace));
            Assertions.assertTrue(script.state("teaselib.household.clothes_pegs").is(script.namespace));

            Assertions.assertTrue(script.state("teaselib.Household.Clothes_Pegs").is(script.namespace));
            Assertions.assertTrue(script.state("teaselib.Household.Clothes_Pegs").is(script.namespace.toLowerCase()));

            Assertions.assertTrue(script.state(Body.OnNipples).is(Household.Clothes_Pegs));
            Assertions.assertTrue(script.state(Body.OnNipples).is("teaselib.Household.Clothes_Pegs"));
            Assertions.assertTrue(script.state(Body.OnNipples).is("TEASELIB.household.cLOTHES_pEGS"));

            Assertions.assertTrue(script.state(Household.Clothes_Pegs).is("teaseLib.Body.OnNipples"));
            Assertions.assertTrue(script.state(Household.Clothes_Pegs).is("teaseLib.body.onnipples"));
        }
    }

    @Test
    public void testCaseIndepencencyOfItems() throws IOException {
        try (TestScript script = new TestScript()) {
            Assertions.assertFalse(script.item(Toys.Collar).applied());
            Assertions.assertTrue(script.item(Toys.Collar).expired());

            script.item(Toys.Collar).apply();
            Assertions.assertTrue(script.item(Toys.Collar).is(Body.AroundNeck));

            Assertions.assertTrue(script.item(Toys.Collar).is(script.namespace));
            Assertions.assertTrue(script.item("teaselib.Toys.Collar").is(script.namespace));
            Assertions.assertTrue(script.item("TeaseLib.toys.collar").is(script.namespace));
            Assertions.assertTrue(script.item("teaselib.toys.COLLAR").is(script.namespace));
            Assertions.assertTrue(script.item("TEASELIB.Toys.cOLLAR").applied());
        }

    }

    @Test
    public void testCaseIndepencencyOfItemAttributes() throws IOException {
        try (TestScript script = new TestScript()) {
            script.item("teaselib.household.clothes_pegs").to("teaseLib.body.onnipples").apply();
            Assertions.assertTrue(script.state(Body.OnNipples).applied());
            Assertions.assertTrue(script.state("TeaseLib.Body.OnNipples").applied());

            Assertions.assertTrue(script.item(Household.Clothes_Pegs).applied());
            Assertions.assertTrue(script.item(Household.Clothes_Pegs).is(script.namespace));
            Assertions.assertTrue(script.item("teaselib.household.clothes_pegs").is(script.namespace));
            Assertions.assertTrue(script.item("teaselib.Household.Clothes_Pegs").is(script.namespace));
            Assertions.assertTrue(script.item("teaselib.Household.Clothes_Pegs").is(script.namespace.toLowerCase()));

            Assertions.assertTrue(script.state(Body.OnNipples).is("teaselib.household.clothes_pegs"));
            Assertions.assertTrue(script.state(Body.OnNipples).is(Household.Clothes_Pegs));
            Assertions.assertTrue(script.state(Body.OnNipples).is("teaselib.Household.Clothes_Pegs"));
            Assertions.assertTrue(script.state(Body.OnNipples).is("TEASELIB.household.cLOTHES_pEGS"));

            Assertions.assertTrue(script.item(Household.Clothes_Pegs).is("teaseLib.Body.OnNipples"));
            Assertions.assertTrue(script.item(Household.Clothes_Pegs).is("teaseLib.body.onnipples"));
        }
    }
}
