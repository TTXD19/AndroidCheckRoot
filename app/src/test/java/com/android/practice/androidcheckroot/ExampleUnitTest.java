package com.android.practice.androidcheckroot;

import android.app.Instrumentation;
import android.content.Context;

import com.scottyab.rootbeer.RootBeer;

import org.junit.Assert;
import org.junit.Test;

import static com.scottyab.rootbeer.Const.BINARY_SU;
import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    private MainActivity mainActivity;

    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
}