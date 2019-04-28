package com.wangrong.stream;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Application {

    public static void main(String[] args) {
        Application application = new Application();
//        application.howToWork();
//        application.creatStream();
//        application.specialStream();
//        application.mapToInt();
//        application.mapToObj();
//        application.executionSequence();
//        application.reuse();
//        application.collect();
//        application.flatMap();
//        application.reduce();
        application.parallel();
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

    /**
     * flatMap 能够将流的每个元素，转换为其他对象的流，因此每个对象可以被转换成>=0个其他对象，并以流的形式返回
     */
    public void flatMap(){
        List<Foo> foos = new ArrayList<>();
        //创建foo集合 包含三个foo
        IntStream.range(1,4).forEach(i -> foos.add(new Foo("Foo"+i)));
        //创建bars集合 每个foo包含三个bar
        foos.forEach(foo -> IntStream.range(1,4).forEach(i -> foo.bars.add(new Bar("Bar"+i+" <- "+ foo.name))));
        foos.stream().flatMap(foo -> foo.bars.stream()).forEach(bar -> System.out.println(bar.name));
        //简化后
        IntStream.range(1,4)
                .mapToObj(i -> new Foo("Foo"+i))
                .peek(foo -> IntStream.range(1,4)
                        .mapToObj(i -> new Bar("Bar"+i+" <- "+ foo.name))
                        .forEach(foo.bars::add))
                .flatMap(foo -> foo.bars.stream())
                .forEach(bar -> System.out.println(bar.name));
        /**
         * flatMap也可用于Java8引入的Optional类。
         * Optional的flatMap操作返回一个Optional或其他类型的对象。所以它可以用于避免繁琐的null检查。
         */
        //为了处理从 Outer 对象中获取最底层的 foo 字符串，你需要添加多个null检查来避免可能发生的NullPointerException
        Outer outer = new Outer();
        if(outer!=null && outer.nested!=null && outer.nested.inner!=null){
            System.out.println(outer.nested.inner.foo);
        }
        //我们还可以使用Optional的flatMap操作，来完成上述相同功能的判断，且更加优雅
        Optional.of(new Outer())
                .flatMap(o -> Optional.ofNullable(o.nested))
                .flatMap(n -> Optional.ofNullable(n.inner))
                .flatMap(i -> Optional.ofNullable(i.foo))
                .ifPresent(System.out::println);
        //如果不为空的话，每个flatMap的调用都会返回预期对象的Optional包装，否则返回为null的Optional包装类。
    }

    /**
     * reduce（减少、归纳为） 规约操作可以将流的所有元素组合成一个结果
     */
    public void reduce(){
        /**
         * 第一种将流中的元素规约成流中的一个元素。
         * reduce方法接受BinaryOperator积累函数。
         * 该函数实际上是两个操作数类型相同的BiFunction。
         * BiFunction功能和Function一样，但是它接受两个参数。
         * 示例代码中，我们比较两个人的年龄，来返回年龄较大的人。
         */
        persons.stream()
                .reduce((p1,p2) -> p1.age > p2.age ? p1 : p2)
                .ifPresent(System.out::println);
        /**
         * 第二种reduce方法接受标识值和BinaryOperator累加器。
         * 此方法可用于构造一个新的 Person，其中包含来自流中所有其他人的聚合名称和年龄：
         */
        Person result = persons.stream()
                .reduce(new Person("",0),(p1,p2) -> {
                    p1.age += p2.age;
                    p1.name += p2.name;
                    return p1;
                });
        System.out.format("name=%s; age=%s", result.name, result.age);
        /**
         * 第三种reduce方法接受三个参数：标识值，BiFunction累加器和类型的组合器函数BinaryOperator。
         * 由于初始值的类型不一定为Person，我们可以使用这个归约函数来计算所有人的年龄总和：
         */
//        Integer ageSum = persons.stream()
//                .reduce(0,
//                        (sum, p) -> {
//                            System.out.format("accumulator: sum=%s; person=%s\n", sum, p);
//                            return sum += p.age;
//                        },
//                        (sum1,sum2) -> {
//                            System.out.format("combiner: sum1=%s; sum2=%s\n", sum1, sum2);
//                            return sum1+sum2;
//                        });
//        System.out.println(ageSum);//这里没有打印组合器调用
        /**
         * 用并行流的方式调用
         * 并行流的执行方式完全不同。这里组合器被调用了。
         * 实际上，由于累加器被并行调用，组合器需要被用于计算部分累加值的总和。
         */
        Integer ageSum = persons
                .parallelStream()
                .reduce(0,
                        (sum, p) -> {
                            System.out.format("accumulator: sum=%s; person=%s\n", sum, p);
                            return sum += p.age;
                        },
                        (sum1,sum2) -> {
                            System.out.format("combiner: sum1=%s; sum2=%s\n", sum1, sum2);
                            return sum1+sum2;
                        });
    }

    /**
     * 并行流
     * 流是可以并行执行的，当流中存在大量元素时，可以显著提升性能
     */
    public void parallel(){
        //线程池最大数
        System.out.println(ForkJoinPool.commonPool().getParallelism());
        //可以设置jvm参数进行修改
        //-Djava.util.concurrent.ForkJoinPool.common.parallelism=5

        /**
         * 集合支持parallelStream()方法来创建元素的并行流。
         * 或者你可以在已存在的数据流上调用中间方法parallel()，将串行流转换为并行流
         */
        Arrays.asList("a1", "a2", "b1", "c2", "c1")
                .parallelStream()
                .filter(s -> {
                    System.out.format("filter: %s [%s]\n",s,Thread.currentThread().getName());
                    return true;
                })
                .map(s -> {
                    System.out.format("map: %s [%s]\n",
                            s, Thread.currentThread().getName());
                    return s.toUpperCase();
                })
                .sorted((s1, s2) -> {
                    System.out.format("sort: %s <> %s [%s]\n",
                            s1, s2, Thread.currentThread().getName());
                    return s1.compareTo(s2);
                })
                .forEach(s -> System.out.format("forEach: %s [%s]\n",
                        s, Thread.currentThread().getName()));
        /**
         * 貌似sort只在主线程上串行执行。
         * 但是实际上，并行流中的sort在底层使用了Java8中新的方法Arrays.parallelSort()
         * 如 javadoc官方文档解释的，这个方法会按照数据长度来决定以串行方式，或者以并行的方式来执行。
         *
         * 所有并行流操作都共享相同的 JVM 相关的公共ForkJoinPool。
         * 所以你可能需要避免写出一些又慢又卡的流式操作，这很有可能会拖慢你应用中，
         * 严重依赖并行流的其它部分代码的性能。
         */
    }

}

class Outer{
    Nested nested;
}

class Nested{
    Inner inner;
}

class Inner{
    String foo;
}

class Foo{
    String name;
    List<Bar> bars = new ArrayList<Bar>();
    Foo(String name){
        this.name = name;
    }
}

class Bar{
    String name;
    Bar(String name){
        this.name = name;
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