package com.lex0v0.piranport.recipe;

// 船坞与开发工厂共用材料扣除契约。
public interface ManufacturingCost {
    int ammunition();
    int steel();
    int aluminum();
}
