### python locust idea 调式
 1. git clone 最新locust代码
 2. 使用pychrom打开
 3. file->settings->python debugger->gevent compatible 勾选
    + 打开gevent debugger
 4. 在locust下写一个main.py
  ``` 
    # This is a sample Python script.
    
    # Press Shift+F10 to execute it or replace it with your code.
    # Press Double Shift to search everywhere for classes, files, tool windows, actions, and settings.
    
    
    from locust.main import main
    
    
    # Press the green button in the gutter to run the script.
    if __name__ == '__main__':
        main()
    
    # See PyCharm help at https://www.jetbrains.com/help/pycharm/

  ```
 5. 启动main配置参数
 ```
    --master --master-bind-host=127.0.0.1 --master-bind-port=5557 --web-host=127.0.0.1 --headless
 ```
 6. 启动go-boomer子节点