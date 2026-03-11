package github.liang118.serialize;

import github.liang118.extension.SPI;

@SPI
public interface Serializer {

    byte[] serialize(Object obj);

    <T> T deserialize(byte[] bytes, Class<T> clazz);

}
