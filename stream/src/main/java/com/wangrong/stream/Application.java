package com.wangrong.stream;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Application {

    public static void main(String[] args) {
        Application application = new Application();
        application.howToWork();
        application.creatStream();
        application.specialStream();
        application.mapToInt();
        application.mapToObj();
    }

    /**
     * 流是如何工作的
     * 流表示包含着一系列的集合，我们可以对其做不同类型的操作，用来对这些元素执行计算
     * 中间操作（filter、map、sorted）会再次返回一个流，终端操作（foreach）是对流操作的一个结束动作
     * 大部分流操作都支持lambda表达式作为参数，或者说是接受一个函数式接口的实现作为参数
     */
    public void howToWork(){
        List<String> list = Arrays.asList("a1", "a2", "a3", "b2", "b1", "c2", "c1");
        list.stream()//创建流
                .filter(s -> s.startsWith("c"))//过滤出以c开头的字符串
                .map(String::toUpperCase)//转换成大写
                .sorted()//排序
                .forEach(System.out::println);//循环打印
    }

    /**
     * 创建流
     */
    public void creatStream(){
        Arrays.asList("a1", "a2", "a3")
                .stream()//创建流
                .findFirst()//找到第一个元素
                .ifPresent(System.out::println);//如果存在，即输出
        Stream.of("a1", "a2", "a3")
                .findFirst()
                .ifPresent(System.out::println);
    }

    /**
     * 特殊对象流 IntStream,LongStream,DoubleStream
     * 与常规对象流的区别
     *  原始类型流使用独有的函数式接口 例如IntFunction代替Function，IntPredicate代替Predicate。
     *  原始类型流支持额外的终端聚合操作，sum()以及average()
     */
    public void specialStream(){
        IntStream.range(1,4)
                .forEach(s -> System.out.print(s+" "));//相当于for (int i = 1; i < 4; i++) {}
        Arrays.stream(new int[]{1, 2, 3})
                .map(n -> 2 * n + 1)//对数值中的每个对象执行2*n+1的操作
                .average()//求平均值
                .ifPresent(System.out::println);
    }

    /**
     * 将常规对象流转换为原始类型流
     */
    public void mapToInt(){
        Stream.of("a1", "a2", "a3")
                .map(s -> s.substring(1))//对每个字符串元素从下标1开始截取
                .mapToInt(Integer::parseInt)//转成int
                .max()//取最大值
                .ifPresent(System.out::println);
    }

    /**
     * 原始类型流转成对象流
     */
    public void mapToObj(){
        IntStream.range(1,4)
                .mapToObj(i -> "a"+i)// for 循环 1->4, 拼接前缀 a
                .forEach(System.out::println);
        Stream.of(1.0, 2.0, 3.0)
                .mapToInt(Double::intValue)//double转int
                .mapToObj(i -> "a"+i)//int转string
                .forEach(System.out::println);
    }

    /**
     * 执行顺序
     * 中间操作重要特性：延迟性 当且仅当存在终端操作时，中间操作才被执行
     */
    public void executionSequence(){
        Stream.of("d2", "a2", "b1", "b3", "c")
                .filter(s -> {
                    System.out.println(s);
                    return true;
                });//不会打印任何内容
        /**
         * 随链条垂直移动的处理
         * 处理第一个元素时，实际上会在执行filter后，执行foreach操作，然后再处理第二个元素
         */
        Stream.of("d2", "a2", "b1", "b3", "c")
                .filter(s -> {
                    System.out.println("filter: "+s);
                    return true;
                })
                .forEach(s -> System.out.println("forEach: "+s));
        /**
         * map:      d2
         * anyMatch: D2
         * map:      a2
         * anyMatch: A2
         * 出于性能考虑，这样设计可以减少对每个元素的实际操作数
         * 由于数据流的链式调用是垂直执行的，map这里只需要执行两次。相对于水平执行，map会执行尽可能少的次数
         */
        Stream.of("d2", "a2", "b1", "b3", "c")
                .map(s -> {
                    System.out.println("map: " + s);
                    return s.toUpperCase(); // 转大写
                })
                .anyMatch(s -> {
                    System.out.println("anyMatch: " + s);
                    return s.startsWith("A"); // 过滤出以 A 为前缀的元素
                });
    }
}
