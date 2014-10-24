package org.push.protocol;

import org.push.util.CppEnum;
import org.push.util.Utils;

/**
 *
 * @author Lei Wang
 */

public class ErrorCodes {

    public static enum ContextStates implements CppEnum {

        InitStarted(1),
        InitEnded(2);

        private int value;

        private ContextStates(int value) { this.value = value; }

        public int value() { return this.value; }
    }

    public static enum EncodeResult implements CppEnum {

        FatalFailure(0),
        Failure(1),
        Success(2),
        InsufficientBufferSpace(3);

        private int value;

        private EncodeResult(int value) { this.value = value; }

        public int value() { return this.value; }
    }

    public static enum SerializeResult implements CppEnum {

        Failure(0),
        InsufficientBufferSpace(1),
        Success(2);

        private int value;

        private SerializeResult(int value) { this.value = value; }

        public int value() { return this.value; }
    }

    public static enum NetworkSerializeResult implements CppEnum {

        FatalFailure(-1),
        Failure(0),
        Retry(1),
        Success(2);

        private int value;

        private NetworkSerializeResult(int value) { this.value = value; }

        public int value() { return this.value; }

        public static NetworkSerializeResult convertEncodingFailure(
                EncodeResult error) {
            Utils.nullArgCheck(error, "error");

            if(error == EncodeResult.FatalFailure) {
                return FatalFailure;
            } else if (error == EncodeResult.Failure) {
                return Failure;
            } else if (error == EncodeResult.InsufficientBufferSpace) {
                return Retry;
            } else {
                //Impossible.
                return Failure;
            }
        }

        public static NetworkSerializeResult convertSerializeFailure(
                SerializeResult error) {
            Utils.nullArgCheck(error, "error");

            if(error == SerializeResult.Failure) {
                return Failure;
            } else if (error == SerializeResult.InsufficientBufferSpace) {
                return Retry;
            } else {
                //Impossible.
                return Failure;
            }
        }
    }


    public static enum DecodeResult implements CppEnum {

        Close(0),
        Failure(1),
        Content(2),
        ProtocolBytes(3),
        WantMoreData(4),
        NoContent(5);

        private int value;

        private DecodeResult(int value) { this.value = value; }

        public int value() { return this.value; }
    }

    public static enum DeserializeResult implements CppEnum {

        Close(0),
        Failure(1),
        Success(2),
        DiscardContent(3);

        private int value;

        private DeserializeResult(int value) { this.value = value; }

        public int value() { return this.value; }
    }

    public static enum NetworkDeserializeResult implements CppEnum {

        Initializationfailure(-2),
        Failure(-1),
        Content(0),
        ProtocolBytes(1),
        WantMoreData(2),
        Close(3);

        private int value;

        private NetworkDeserializeResult(int value) { this.value = value; }

        public int value() { return this.value; }

        public static boolean isFailure(NetworkDeserializeResult error) {
            Utils.nullArgCheck(error, "error");
            return error == Failure || error == Initializationfailure;
        }
    }

}
