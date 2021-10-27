package teaselib.core;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import teaselib.Body;
import teaselib.Household;
import teaselib.Toys;
import teaselib.test.TestScript;

public class StateCaseIndepencyTests {

    @Test
    public void testCaseIndepencencyOfState() throws IOException {
        try (TestScript script = new TestScript()) {
            assertFalse(script.state(Toys.Collar).applied());
            assertTrue(script.state(Toys.Collar).expired());

            script.state(Toys.Collar).applyTo();

            assertTrue(script.state(Toys.Collar).is(script.namespace));
            assertTrue(script.state("teaselib.Toys.Collar").is(script.namespace));
            assertTrue(script.state("TeaseLib.toys.collar").is(script.namespace));
            assertTrue(script.state("teaselib.toys.COLLAR").is(script.namespace));
            assertTrue(script.state("TEASELIB.Toys.cOLLAR").applied());
        }

    }

    @Test
    public void testCaseIndepencencyOfStatePeersAndAttributes() throws IOException {
        try (TestScript script = new TestScript()) {
            script.state("teaselib.household.clothes_pegs").applyTo("teaseLib.body.onnipples");
            assertTrue(script.state(Body.OnNipples).applied());
            assertTrue(script.state("TeaseLib.Body.OnNipples").applied());

            assertTrue(script.state(Household.Clothes_Pegs).applied());
            assertTrue(script.state(Household.Clothes_Pegs).is(script.namespace));
            assertTrue(script.state("teaselib.household.clothes_pegs").is(script.namespace));

            assertTrue(script.state("teaselib.Household.Clothes_Pegs").is(script.namespace));
            assertTrue(script.state("teaselib.Household.Clothes_Pegs").is(script.namespace.toLowerCase()));

            assertTrue(script.state(Body.OnNipples).is(Household.Clothes_Pegs));
            assertTrue(script.state(Body.OnNipples).is("teaselib.Household.Clothes_Pegs"));
            assertTrue(script.state(Body.OnNipples).is("TEASELIB.household.cLOTHES_pEGS"));

            assertTrue(script.state(Household.Clothes_Pegs).is("teaseLib.Body.OnNipples"));
            assertTrue(script.state(Household.Clothes_Pegs).is("teaseLib.body.onnipples"));
        }
    }

    @Test
    public void testCaseIndepencencyOfItems() throws IOException {
        try (TestScript script = new TestScript()) {
            assertFalse(script.item(Toys.Collar).applied());
            assertTrue(script.item(Toys.Collar).expired());

            script.item(Toys.Collar).apply();
            assertTrue(script.item(Toys.Collar).is(Body.AroundNeck));

            assertTrue(script.item(Toys.Collar).is(script.namespace));
            assertTrue(script.item("teaselib.Toys.Collar").is(script.namespace));
            assertTrue(script.item("TeaseLib.toys.collar").is(script.namespace));
            assertTrue(script.item("teaselib.toys.COLLAR").is(script.namespace));
            assertTrue(script.item("TEASELIB.Toys.cOLLAR").applied());
        }

    }

    @Test
    public void testCaseIndepencencyOfItemAttributes() throws IOException {
        try (TestScript script = new TestScript()) {
            script.item("teaselib.household.clothes_pegs").applyTo("teaseLib.body.onnipples");
            assertTrue(script.state(Body.OnNipples).applied());
            assertTrue(script.state("TeaseLib.Body.OnNipples").applied());

            assertTrue(script.item(Household.Clothes_Pegs).applied());
            assertTrue(script.item(Household.Clothes_Pegs).is(script.namespace));
            assertTrue(script.item("teaselib.household.clothes_pegs").is(script.namespace));
            assertTrue(script.item("teaselib.Household.Clothes_Pegs").is(script.namespace));
            assertTrue(script.item("teaselib.Household.Clothes_Pegs").is(script.namespace.toLowerCase()));

            assertTrue(script.state(Body.OnNipples).is("teaselib.household.clothes_pegs"));
            assertTrue(script.state(Body.OnNipples).is(Household.Clothes_Pegs));
            assertTrue(script.state(Body.OnNipples).is("teaselib.Household.Clothes_Pegs"));
            assertTrue(script.state(Body.OnNipples).is("TEASELIB.household.cLOTHES_pEGS"));

            assertTrue(script.item(Household.Clothes_Pegs).is("teaseLib.Body.OnNipples"));
            assertTrue(script.item(Household.Clothes_Pegs).is("teaseLib.body.onnipples"));
        }
    }
}
