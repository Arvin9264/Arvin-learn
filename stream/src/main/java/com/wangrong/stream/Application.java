package com.wangrong.stream;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
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
        application.executionSequence();
        application.reuse();
        application.collect();
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

    /**
     * stream 复用
     */
    public void reuse(){
        //Stream流不能复用，调用终端操作后流即关闭
        Stream<String> stream =
                Stream.of("d2", "a2", "b1", "b3", "c")
                        .filter(s -> s.startsWith("a"));
        stream.anyMatch(s -> true);    // ok
//        stream.noneMatch(s -> true);   // java.lang.IllegalStateException: stream has already been operated upon or closed
        //为想要执行的每个终端操作创建一个新的流链,可以通过Supplier包装流
        Supplier<Stream<String>> streamSupplier =
                () -> Stream.of("d2", "a2", "b1", "b3", "c")
                        .filter(s -> s.startsWith("a"));
        streamSupplier.get().anyMatch(s -> true);
        streamSupplier.get().noneMatch(s -> true);
    }

    List<Person> persons = Arrays.asList(
            new Person("Arvin",23),
            new Person("小平",22),
            new Person("增甫",23),
            new Person("大豪",16)
    );

    /**
     * collect可以将流中的元素转变成另一个不同的对象
     * collect 接受入参为Collector（收集器），
     * 它由四个不同的操作组成：供应器（supplier）、累加器（accumulator）、组合器（combiner）和终止器（finisher）。
     */
    public void collect(){
        //从流中构造List
        List<Person> filtered = persons.stream()
                .filter(person -> person.name.startsWith("A"))
                .collect(Collectors.toList());
        System.out.println(filtered);
        //以年龄为 key,进行分组
        Map<Integer, List<Person>> personsByAge = persons
                .stream()
                .collect(Collectors.groupingBy(p -> p.age));
        personsByAge.forEach((age, p) -> System.out.format("age %s: %s\n", age, p));
        //在流上执行聚合操作：计算所有人的平均年龄
        Double averageAge = persons
                .stream()
                .collect(Collectors.averagingInt(p -> p.age));
        System.out.println(averageAge);
        //更全面的统计信息，摘要收集器，可以返回一个特殊的内置统计对象（count、sum、min、average、max）
        IntSummaryStatistics ageSummary = persons
                .stream()
                .collect(Collectors.summarizingInt(p -> p.age));
        System.out.println(ageSummary);
        //连接收集器,将所有人名连成一个字符串
        String phrase = persons
                .stream()
                .map(person -> person.name)
                .collect(Collectors.joining(" and "));
        System.out.println(phrase);
        /**
         * 对于如何将流转换为 Map集合，我们必须指定 Map 的键和值。
         * 这里需要注意，Map 的键必须是唯一的，否则会抛出IllegalStateException 异常。
         */
        Map<Integer, String> map = persons
                .stream()
                .collect(Collectors.toMap(
                        p -> p.age,
                        p -> p.name,
                        (name1, name2) -> name1 + ";" + name2)); // 对于同样 key 的，将值拼接
        System.out.println(map);
        /**
         * 构建自定义收集器
         * 比如说，我们希望将流中的所有人转换成一个字符串，包含所有大写的名称，并以|分割。
         * 通过Collector.of()创建一个新的收集器
         * 由于Java 中的字符串是 final 类型的，我们需要借助辅助类StringJoiner，来帮我们构造字符串。
         * 1.最开始供应器使用分隔符构造了一个StringJointer。
         * 2.累加器用于将每个人的人名转大写，然后加到StringJointer中。
         * 3.组合器将两个StringJointer合并为一个。
         * 4.最终，终结器从StringJointer构造出预期的字符串。
         */
        Collector<Person, StringJoiner,String> personStringJoinerStringCollector =
                Collector.of(
                        () -> new StringJoiner("|"),//supplier 供应器
                        (j, p) -> j.add(p.name.toUpperCase()),//accumulator累加器
                        (j1, j2) -> j1.merge(j2),             //combiner组合器
                        StringJoiner::toString                //finisher终止器
                );
        String names = persons.stream().collect(personStringJoinerStringCollector);//传入自定义的收集器
        System.out.println(names);
    }



}
class Person{
    String name;
    int age;
    Person(String name,int age){
        this.name = name;
        this.age = age;
    }
    public String toString(){
        return name;
    }
}