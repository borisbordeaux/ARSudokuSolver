# -*- coding: utf-8 -*
import cv2
import numpy as np
import os
import pickle
import tensorflow as tf

from perlin_numpy import generate_perlin_noise_2d
from PIL import ImageFont, ImageDraw, Image
from sklearn.utils import shuffle
from sklearn.model_selection import train_test_split
from tensorflow import keras
from tensorflow.python.framework.convert_to_constants import convert_variables_to_constants_v2
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Flatten, Conv2D, MaxPooling2D


def main():
    print('Main function')
    # uncomment the line you want to execute
    # generate_dataset_using_ttf(nbr=30, save_files=True)
    # main_train_model(from_files=True)
    # test_loaded_model()
    # test_noise()


def main_train_model(from_files=True):
    train_images, train_labels, test_images, test_labels = load_dataset(from_files)
    model = create_model()
    cv2.destroyAllWindows()

    print('train_images shape:', train_images.shape)
    print('train_labels shape:', train_labels.shape)
    print('test_images shape:', test_images.shape)
    print('test_labels shape:', test_labels.shape)

    trained_model = fit_model(train_images, train_labels, test_images, test_labels, model)
    print('Begin test_ai')
    test_ai(trained_model, test_images, test_labels)
    print('Begin save model')
    save_fit_model(model)


def test_noise():
    while True:
        im, tx, ty = gen_full_noisy_image(np.random.randint(0, 66) / 100.0, add_perlin_noise=True, add_border=False)
        cv2.imshow("test", im)
        if cv2.waitKey() & 0xFF == ord('q'):
            break
    cv2.destroyAllWindows()


def translate_image(im, x, y):
    matrix = np.float32([[1, 0, x], [0, 1, y]])
    shifted = cv2.warpAffine(im, matrix, (im.shape[1], im.shape[0]))
    return shifted


def rotate_image(im, angle, center, scale):
    matrix = cv2.getRotationMatrix2D(center, angle, scale)
    rotated = cv2.warpAffine(im, matrix, (im.shape[1], im.shape[0]))
    return rotated


def add_black_border(img, val=4, size=28):
    """
    Set the border in black
    """
    shape_val = size - 2 * val
    out = cv2.copyMakeBorder(img[val:shape_val + val, val:shape_val + val],
                             val, val, val, val, cv2.BORDER_CONSTANT, None, 0)
    return out


def gen_perlin_noisy_image(thresh=0.3):
    """
    Generates a 28x28 image with perlin noise
    :param thresh: the thresh, good result between 0.0 and 0.6,
    set 1 to generate a black image, 0 to generate a white image
    :return: 28x28 image with perlin noise
    """
    noise = generate_perlin_noise_2d((240, 240), (12, 12))

    # thresholding
    noise[noise > thresh] = 1
    noise[noise < thresh] = 0

    noise = cv2.resize(noise, dsize=(28, 28), interpolation=cv2.INTER_NEAREST)

    noise[noise > 0.1] = 1
    noise[noise < 0.1] = 0

    return noise


def gen_rect_image():
    size = 32

    img = np.ones((size, size))

    val = 4
    shape_val = size - 2 * val
    img[val:shape_val + val, val:shape_val + val] = 0

    img = rotate_image(img, np.random.randint(-3, 4), (size // 2, size // 2), 1. + (np.random.rand()) * 0.3)
    trans_x = np.random.randint(-2, 3)
    trans_y = np.random.randint(-2, 3)
    img = translate_image(img, trans_x, trans_y)

    thresh = 0.1

    img[img > thresh] = 1
    img[img < thresh] = 0

    return img[2:30, 2:30], trans_x, trans_y


def gen_full_noisy_image(thresh, add_perlin_noise=True, add_border=False):
    noise = gen_perlin_noisy_image(thresh) if add_perlin_noise else np.zeros((28, 28))
    rect, t_x, t_y = gen_rect_image()
    img = cv2.add(noise, rect)
    # to avoid values above 1 after add
    img = np.clip(img, 0.0, 1.0)
    if add_border:
        img = add_black_border(img)
    return img, t_x, t_y


# noinspection PyTypeChecker,PyArgumentList
def generate_dataset_using_ttf(nbr=40, save_files=False):
    """
    Generate dataset using font files
    Total image generated = 7590 * nbr
    """
    tab_images = []
    tab_labels = []

    # for each font
    for root, dirs, files in os.walk("./fonts"):
        for file in files:
            if file.endswith("ttf") or file.endswith("TTF"):
                print(root + "/" + file)

                # for each number in [1..9]
                for i in range(1, 10):
                    # we generate nbr noisy images and nbr not so noisy images
                    # and nbr no noisy images
                    for cpt in range(3 * nbr):
                        # create the 28*28 noisy image for 1st part, little noise for 2nd and no noise for 3rd
                        image_m, t_x, t_y = gen_full_noisy_image((np.random.randint(0, 20) / 100.0) if cpt < nbr
                                                                 else (np.random.randint(20, 66) / 100.0),
                                                                 add_perlin_noise=cpt < 2 * nbr)

                        image_pil = Image.fromarray(image_m)

                        # prepare to draw inside the image
                        draw = ImageDraw.Draw(image_pil)

                        # load the font with a random size
                        font_size = np.random.randint(24, 32)
                        font = ImageFont.truetype(root + "/" + file, font_size)

                        # the text to draw is the current number
                        text = "{:d}".format(i)

                        # draw the text in white in the image
                        x = 14 + t_x
                        y = 14 + t_y

                        step = 2

                        # thin border
                        draw.text((x - step, y), text, font=font, anchor="mm", fill=0)
                        draw.text((x + step, y), text, font=font, anchor="mm", fill=0)
                        draw.text((x, y - step), text, font=font, anchor="mm", fill=0)
                        draw.text((x, y + step), text, font=font, anchor="mm", fill=0)

                        # thicker border
                        draw.text((x - step, y - step), text, font=font, anchor="mm", fill=0)
                        draw.text((x + step, y - step), text, font=font, anchor="mm", fill=0)
                        draw.text((x - step, y + step), text, font=font, anchor="mm", fill=0)
                        draw.text((x + step, y + step), text, font=font, anchor="mm", fill=0)

                        draw.text((x, y), text, font=font, anchor="mm", fill=1.0)

                        # create the numpy image of the drawn one
                        # image = add_black_border(np.array(image_pil))
                        image = np.array(image_pil)

                        # ret, image = cv2.threshold(image, 0.1, 1.0, cv2.THRESH_BINARY)
                        # cv2.imshow('chiffre', image.reshape(28, 28, 1))
                        # cv2.waitKey()

                        # append image and label to the lists
                        tab_images.append(image.reshape(28, 28, 1))
                        tab_labels.append(i)

                # for the number 0, we generate nbr noisy images, nbr not so noisy images and nbr no noisy images
                for cpt in range(3 * nbr):
                    image_m, t_x, t_y = gen_full_noisy_image((np.random.randint(0, 20) / 100.0) if cpt < nbr
                                                             else (np.random.randint(20, 66) / 100.0),
                                                             add_perlin_noise=cpt < 2 * nbr)
                    # cv2.imshow('chiffre', image_m.reshape(28, 28, 1))
                    # cv2.waitKey()
                    tab_images.append(image_m.reshape(28, 28, 1))
                    tab_labels.append(0)

    tab_images = np.array(tab_images)
    tab_labels = np.array(tab_labels)

    print(tab_labels)

    tab_images, tab_labels = shuffle(tab_images, tab_labels)

    if True:  # Set to True to see generated images
        for i in range(len(tab_images)):
            cv2.imshow('chiffre', tab_images[i].reshape(28, 28, 1))
            print(tab_labels[i])
            if cv2.waitKey() & 0xFF == ord('q'):
                break

    print("Nbr:", len(tab_images))

    train_images, test_images, train_labels, test_labels = train_test_split(tab_images, tab_labels, test_size=0.20)

    # saving dataset in files
    if save_files:
        with open('x_train.pickle', 'wb') as f:
            pickle.dump(train_images, f)

        with open('y_train.pickle', 'wb') as f:
            pickle.dump(train_labels, f)

        with open('x_test.pickle', 'wb') as f:
            pickle.dump(test_images, f)

        with open('y_test.pickle', 'wb') as f:
            pickle.dump(test_labels, f)

    return train_images, train_labels, test_images, test_labels


def load_dataset_from_files():
    with open('x_train.pickle', 'rb') as f:
        x_train = pickle.load(f)

    with open('y_train.pickle', 'rb') as f:
        y_train = pickle.load(f)

    with open('x_test.pickle', 'rb') as f:
        x_test = pickle.load(f)

    with open('y_test.pickle', 'rb') as f:
        y_test = pickle.load(f)

    return x_train, y_train, x_test, y_test


def load_dataset(from_files=True):
    x_train, y_train, x_test, y_test = load_dataset_from_files() if from_files else generate_dataset_using_ttf()

    y_train = keras.utils.to_categorical(y_train, num_classes)
    y_test = keras.utils.to_categorical(y_test, num_classes)

    print('x_train shape:', x_train.shape)
    print('y_train shape:', y_train.shape)
    print('x_test shape:', x_test.shape)
    print('y_test shape:', y_test.shape)

    return x_train, y_train, x_test, y_test


def create_model():
    model_ai = Sequential()
    # images 28 x 28 output -> images 32 x 26 x 26 (boundary effects)
    model_ai.add(Conv2D(32, kernel_size=(3, 3), activation='relu',
                        kernel_initializer='he_uniform', input_shape=(28, 28, 1)))

    # deactivate random outputs
    # to make the error going to other paths
    # upgrade the network generalisation
    # not used for inferences
    # model_ai.add(Dropout(0.3))

    # reduces the size of the image using max pooling algorithm
    # output -> 13 x 13
    model_ai.add(MaxPooling2D(pool_size=(2, 2)))

    # images 32 x 13 x 13 -> output 32 x 11 x 11 (boundary effects)
    model_ai.add(Conv2D(64, kernel_size=(3, 3), activation='relu', kernel_initializer='he_uniform'))

    # deactivate random outputs
    # model_ai.add(Dropout(0.4))

    # images 32 x 11 x 11 -> output 32 x 5 x 5
    model_ai.add(MaxPooling2D(pool_size=(2, 2)))

    # generate only one output vector
    # images 32 x 5 x 5 -> 800 outputs
    model_ai.add(Flatten())

    # 800 inputs -> 128 outputs
    model_ai.add(Dense(128, activation='relu'))

    # deactivate random outputs
    # model_ai.add(Dropout(0.5))

    # 128 inputs -> 10 outputs with softmax function
    model_ai.add(Dense(num_classes, activation='softmax'))

    # check the net
    # specification of the metrics, optimizer etc...
    model_ai.compile(loss=keras.losses.categorical_crossentropy,
                     optimizer=keras.optimizers.Adam(),
                     metrics=['accuracy'])

    # net summary
    model_ai.summary()

    return model_ai


def fit_model(x_train, y_train, x_test, y_test, model_ai):
    # learning
    model_ai.fit(x_train, y_train, batch_size=batch_size, epochs=epochs, verbose=1, validation_data=(x_test, y_test))

    # evaluation on test dataset
    score = model_ai.evaluate(x_test, y_test, verbose=0)

    print('test loss:', score[0])
    print('test accuracy:', score[1])

    return model_ai


def save_fit_model(model_ai):
    # save of the model to the format SavedModel
    tf.saved_model.save(model_ai, 'trained_Model')

    # convert keras to concrete functions
    full_model = tf.function(lambda x: model_ai(x))
    full_model = full_model.get_concrete_function(x=tf.TensorSpec(model_ai.inputs[0].shape,
                                                                  model_ai.inputs[0].dtype))

    # get frozen concrete function
    frozen_func = convert_variables_to_constants_v2(full_model)

    tf.io.write_graph(frozen_func.graph, "frozen_models", "frozen_graph.pb", as_text=False)
    # tf.io.write_graph(frozen_func.graph, "frozen_models", "frozen_graph.pbtxt", as_text=True)


def test_ai(model_ai, x_test, y_test):
    cv2.namedWindow("test")

    test_imgs = []
    for i in range(100):
        test_imgs.append(x_test[i])

    test_imgs = np.array(test_imgs)

    prediction = model_ai.predict(test_imgs)

    for i in range(100):
        print(y_test[i], np.argmax(y_test[i]), np.argmax(prediction[i]))
        cv2.imshow("test", x_test[i])
        if cv2.waitKey() & 0xFF == ord('q'):
            break

    cv2.destroyAllWindows()


def test_loaded_model():
    loaded_model = cv2.dnn.readNetFromTensorflow("./frozen_models/frozen_graph.pb")

    for i in range(80):
        my_img = cv2.imread(f'../images2/{i}.jpg', cv2.IMREAD_GRAYSCALE)
        img = cv2.resize(my_img, (28, 28))
        # img = add_black_border(img).reshape(28, 28, 1)

        ret, img = cv2.threshold(img, 100, 255, cv2.THRESH_BINARY)

        img_in = img.astype('float32') / 255.

        blob = cv2.dnn.blobFromImage(img_in.reshape(28, 28, 1))

        loaded_model.setInput(blob)
        loaded_pred = loaded_model.forward()

        pred = np.argmax(loaded_pred)
        print(pred, loaded_pred[0, pred], "\n")

        cv2.imshow("test", img)
        if cv2.waitKey() & 0xFF == ord('q'):
            break

    cv2.destroyAllWindows()


# deactivate some logs
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'

# to be sure we use GPU
physical_devices = tf.config.experimental.list_physical_devices('GPU')
assert len(physical_devices) > 0, "Not enough GPU hardware devices available"

# to use only required memory on gpu, and not use all memory
# sometimes required on some GPU
tf.config.experimental.set_memory_growth(physical_devices[0], True)

# AI hyper parameters
num_classes = 10  # from 0 to 9
batch_size = 128  # how many images are inferred before adjusting weights
epochs = 20  # number of train step

# call the main function, at the top of the file for good visibility
main()
