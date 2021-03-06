/**
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.db.service.impl;

import io.nuls.core.model.Result;
import io.nuls.db.manager.LevelDBManager;
import io.nuls.db.service.intf.KVStorageService;

/**
 * @Desription:
 * @Author: PierreLuo
 * @Date: 2018/4/18
 */
public class LevelDBStorageServiceImpl implements KVStorageService {


    @Override
    public Result createArea(String areaName) {
        return LevelDBManager.createArea(areaName);
    }

    @Override
    public String[] listArea() {
        return LevelDBManager.listArea();
    }

    @Override
    public Result put(String area, byte[] key, byte[] value) {
        return LevelDBManager.put(area, key, value);
    }

    @Override
    public Result put(String area, String key, String value) {
        return LevelDBManager.put(area, key, value);
    }

    @Override
    public Result put(String area, byte[] key, String value) {
        return LevelDBManager.put(area, key, value);
    }

    @Override
    public Result delete(String area, String key) {
        return LevelDBManager.delete(area, key);
    }

    @Override
    public byte[] get(String area, String key) {
        return LevelDBManager.get(area, key);
    }

    @Override
    public byte[] get(String area, byte[] key) {
        return LevelDBManager.get(area, key);
    }
}
